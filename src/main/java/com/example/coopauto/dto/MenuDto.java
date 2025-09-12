package com.example.coopauto.dto;

import lombok.Data;
import java.util.List;

@Data
public class MenuDto {
    private int year;                 // 입력한 년도
    private String cafeteria;         // 식당명
    private String rawText;           // 붙여넣은 원본 텍스트
    private List<SelectedMealDto> selectedMeals; // 선택된 끼니 리스트
}
