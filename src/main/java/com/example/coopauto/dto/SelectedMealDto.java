package com.example.coopauto.dto;

import lombok.Data;

@Data
public class SelectedMealDto {
    private String day;   // mon, tue, wed … (요일)
    private String meal;  // breakfast, lunch, lunch_1, lunch_2, dinner …
}
