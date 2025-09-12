package com.example.coopauto.controller;

import com.example.coopauto.dto.MenuDto;
import com.example.coopauto.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // 첫 화면
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("inputText", "");
        model.addAttribute("resultText", "");
        return "index";
    }

    // AJAX 변환 처리
    @PostMapping("/convert-ajax")
    @ResponseBody
    public ResponseEntity<String> convertAjax(@RequestBody MenuDto menuDto) {
        String result = menuService.convertMenu(menuDto);
        return ResponseEntity.ok(result);
    }
}
