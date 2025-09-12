package com.example.coopauto.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemDto {
    private String name;   // 메뉴명
    private int price;     // 식단가
    private int cost;      // 원가
    private int eaters;    // 식수
}
