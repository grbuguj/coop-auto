package com.example.coopauto.service;

import com.example.coopauto.dto.MenuDto;
import com.example.coopauto.dto.MenuItemDto;
import com.example.coopauto.dto.SelectedMealDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class MenuService {

    public String convertMenu(MenuDto request) {
        int year = request.getYear();
        String cafeteria = request.getCafeteria();
        String rawText = request.getRawText();

        List<SelectedMealDto> selectedMeals = request.getSelectedMeals();
        List<LocalDate> dates = extractDatesFromHeader(year, rawText);
        List<MenuItemDto> menuItems = parseMenuItems(rawText);

        // 선택 개수와 파싱 개수가 다르면 에러
        if (menuItems.size() != selectedMeals.size()) {
            return "에러: 선택한 끼니 개수(" + selectedMeals.size() +
                    ")와 파싱된 메뉴 개수(" + menuItems.size() + ")가 다릅니다.";
        }

        // 매핑
        List<MenuRecord> records = new ArrayList<>();
        for (int i = 0; i < selectedMeals.size(); i++) {
            SelectedMealDto mealDto = selectedMeals.get(i);
            MenuItemDto menuItem = menuItems.get(i);

            // day(mon/tue/...) 기준으로 date 찾기
            Optional<LocalDate> matchedDate = dates.stream()
                    .filter(d -> d.getDayOfWeek().toString().toLowerCase().startsWith(mealDto.getDay()))
                    .findFirst();

            if (matchedDate.isEmpty()) continue;
            LocalDate date = matchedDate.get();
            int weekOfMonth = (date.getDayOfMonth() - 1) / 7 + 1;

            records.add(new MenuRecord(
                    year, date, weekOfMonth,
                    cafeteria, mealDto.getMeal(), menuItem
            ));
        }

        // 날짜 + 끼니순 정렬
        records.sort(Comparator
                .comparing(MenuRecord::getDate)
                .thenComparing(MenuRecord::getMealOrder));

        // 출력
        StringBuilder sb = new StringBuilder();
        for (MenuRecord rec : records) {
            String mealKor = toKoreanMealName(rec.meal);
            String corner = mapCorner(rec.meal);
            String mealType = rec.menuItem.getName().contains("국밥") ? "국밥" : "백반";



            sb.append(String.format(
                    "%d\t%d\t%d\t%s\t%d\t%s\t%s\t%s\t%s\t%s\t%d\t%d\t\t%d%n",
                    rec.year,
                    rec.date.getMonthValue(),
                    rec.date.getDayOfMonth(),
                    rec.date.getDayOfWeek().toString().toLowerCase().substring(0, 3),
                    rec.weekOfMonth,
                    rec.cafeteria,
                    mealKor,
                    corner,
                    mealType,
                    rec.menuItem.getName(),
                    rec.menuItem.getPrice(),
                    rec.menuItem.getCost(),   // 원가
                    rec.menuItem.getEaters()  // 예상식수
            ));


        }

        return sb.toString();
    }

    // 내부 레코드
    private static class MenuRecord {
        int year;
        LocalDate date;
        int weekOfMonth;
        String cafeteria;
        String meal;
        MenuItemDto menuItem;

        MenuRecord(int year, LocalDate date, int weekOfMonth,
                   String cafeteria, String meal, MenuItemDto menuItem) {
            this.year = year;
            this.date = date;
            this.weekOfMonth = weekOfMonth;
            this.cafeteria = cafeteria;
            this.meal = meal;
            this.menuItem = menuItem;
        }

        int getMealOrder() {
            return switch (meal) {
                case "breakfast" -> 1;
                case "lunch", "lunch_1", "lunch_2", "lunch_3", "lunch_4", "lunch_5a", "lunch_5b" -> 2;
                case "dinner" -> 3;
                default -> 99;
            };
        }

        LocalDate getDate() {
            return date;
        }
    }

    // 날짜 헤더 파싱
    private List<LocalDate> extractDatesFromHeader(int year, String rawText) {
        List<LocalDate> result = new ArrayList<>();
        String[] lines = rawText.split("\n");

        for (String line : lines) {
            if (line.matches(".*\\d{2}/\\d{2}.*")) {
                String[] tokens = line.trim().split("\\s+");
                for (String token : tokens) {
                    if (token.matches("\\d{2}/\\d{2}.*")) {
                        String[] md = token.split("/");
                        int month = Integer.parseInt(md[0]);
                        int day = Integer.parseInt(md[1]);
                        result.add(LocalDate.of(year, month, day));
                    }
                }
            }
        }
        return result;
    }

    // 메뉴 블럭 파싱 (대표메뉴 + 숫자세트 매칭)
    // 메뉴 블럭 파싱 (대표메뉴 + 숫자세트 매칭)
    private List<MenuItemDto> parseMenuItems(String rawText) {
        List<MenuItemDto> result = new ArrayList<>();

        // 1. 끼니별 섹션 분리
        String[] mealSections = rawText.split("(?=조식|중식|석식|매점)");

        for (String section : mealSections) {
            section = section.trim();
            if (section.isEmpty()) continue;

            // [예외처리] 매점/그린샐러드는 건너뛰기
            if (section.startsWith("매점") || section.contains("그린샐러드")) {
                continue;
            }

            // 2. 대표메뉴 추출 (블럭마다 첫 번째 ■)
            List<String> repMenus = new ArrayList<>();
            String[] blocks = section.split("\\n\\s*\\n");
            for (String block : blocks) {
                String[] lines = block.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("■")) {
                        String repMenu = line.replace("■", "")
                                .replaceAll("\\(.*?\\)", "")   // 괄호 제거
                                .replaceAll("\\s*\\d+$", "")  // 끝 숫자 제거
                                .trim();

                        // [예외처리] 대표메뉴가 "그린샐러드"면 skip
                        if (repMenu.contains("그린샐러드")) {
                            continue;
                        }

                        repMenus.add(repMenu);
                        break; // 블록에서 첫 번째 메뉴만
                    }
                }
            }

            // 3. 숫자세트 추출 (칼로리~비율까지 하나의 세트로 처리)
            List<int[]> numbers = new ArrayList<>();
            StringBuilder numBlock = new StringBuilder();
            for (String line : section.split("\n")) {
                line = line.trim();
                if (line.startsWith("칼로리") || line.startsWith("식수")
                        || line.startsWith("원가") || line.startsWith("식단가")
                        || line.startsWith("비율")) {
                    numBlock.append(line).append("\n");

                    if (line.startsWith("비율")) { // 세트 끝
                        String block = numBlock.toString();
                        numBlock.setLength(0);

                        int price = extractValue(block, "식단가");
                        int cost = extractValue(block, "원가");
                        int eaters = extractValue(block, "식수");

                        numbers.add(new int[]{price, cost, eaters});
                    }
                }
            }

            // 4. 대표메뉴와 숫자세트 매핑
            int limit = Math.min(repMenus.size(), numbers.size());
            for (int i = 0; i < limit; i++) {
                int[] nums = numbers.get(i);
                result.add(new MenuItemDto(repMenus.get(i), nums[0], nums[1], nums[2]));
            }

            // 남은 대표메뉴는 0으로 채움
            for (int i = limit; i < repMenus.size(); i++) {
                result.add(new MenuItemDto(repMenus.get(i), 0, 0, 0));
            }
        }

        return result;
    }


    // 숫자블록에서 특정 키의 값 추출
    private int extractValue(String block, String key) {
        for (String line : block.split("\n")) {
            if (line.contains(key)) {
                return safeParseInt(line);
            }
        }
        return 0;
    }

    private int safeParseInt(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    // 끼니 이름 변환
    private String toKoreanMealName(String meal) {
        return switch (meal) {
            case "breakfast" -> "조식";
            case "lunch", "lunch_1" -> "중식";
            case "lunch_2" -> "중식";
            case "lunch_3" -> "중식";
            case "lunch_4" -> "중식";
            case "lunch_5a", "lunch_5b" -> "중식";
            case "dinner" -> "석식";
            default -> meal;
        };
    }

    // 코너 매핑
    private String mapCorner(String meal) {
        return switch (meal) {
            case "lunch_1" -> "1코너";
            case "lunch_2" -> "2코너";
            case "lunch_3" -> "3코너";
            case "lunch_4" -> "4코너";
            case "lunch_5a" -> "5코너(A)";
            case "lunch_5b" -> "5코너(B)";
            default -> "1코너";
        };
    }

    // 메뉴 타입 매핑
    // 메뉴 타입 매핑 (대표메뉴명 기반)
    private String mapType(MenuItemDto menuItem) {
        String name = menuItem.getName();
        if (name != null && name.contains("국밥")) {
            return "국밥";
        }
        return "백반";
    }

}
