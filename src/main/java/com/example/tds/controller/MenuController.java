package com.example.tds.controller;

import com.example.tds.dto.requests.menu.CreateMenuRequest;
import com.example.tds.dto.requests.menu.UpdateMenuRequest;
import com.example.tds.dto.responses.ApiResponse;
import com.example.tds.dto.responses.ChefResponse;
import com.example.tds.dto.responses.MenuResponse;
import com.example.tds.entity.ChefEntity;
import com.example.tds.mapper.ChefMapper;
import com.example.tds.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chefs/{chefId}/menus")
public class MenuController {
    private final MenuService menuService;
    private final ChefMapper chefMapper;

    @PostMapping()
    public ResponseEntity<ApiResponse> menuCreator(@RequestAttribute("chef") ChefEntity chef, @RequestBody @Valid CreateMenuRequest createMenuRequest) {
        MenuResponse menuResponse = menuService.handleMenuCreation(chef, createMenuRequest);

        ChefResponse chefResponse = chefMapper.toChefResponse(chef);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.builder()
                        .message("menu created successfully")
                        .success(true)
                        .data(Map.of("menu", menuResponse, "chef", chefResponse))
                        .build()
        );
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse> bulkMenuCreation(@RequestAttribute("chef") ChefEntity chef, @RequestBody @Valid List<CreateMenuRequest> createMenuRequestList){
        List<MenuResponse> menuResponses = menuService.handleMultipleMenuCreation(chef, createMenuRequestList);

        ChefResponse chefResponse = chefMapper.toChefResponse(chef);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.builder()
                    .message("menus created successfully")
                    .success(true)
                    .data(Map.of("menus", menuResponses, "chef", chefResponse))
                    .build()
        );
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<ApiResponse> getMenu(@RequestAttribute("chef") ChefEntity chef, @PathVariable("menuId")UUID menuId){
        MenuResponse menuResponse = menuService.handleGetMenu(menuId);

        ChefResponse chefResponse = chefMapper.toChefResponse(chef);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("menu found successfully")
                        .success(true)
                        .data(Map.of("menu", menuResponse, "chef", chefResponse))
                        .build()
        );
    }

    @GetMapping()
    public ResponseEntity<ApiResponse> getAllMenus(@RequestAttribute("chef") ChefEntity chef){
        List<MenuResponse> menuResponses = menuService.handleGetAllMenus(chef.getChefId());

        ChefResponse chefResponse = chefMapper.toChefResponse(chef);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("menus found successfully")
                        .success(true)
                        .data(Map.of("menus", menuResponses, "chef", chefResponse))
                        .build()
        );
    }

    @PatchMapping("/{menuId}")
    public ResponseEntity<ApiResponse> updateMenu(@RequestAttribute("chef") ChefEntity chef, @PathVariable("menuId") UUID menuId, @RequestBody @Valid UpdateMenuRequest updateMenuRequest) {
        MenuResponse menuResponse = menuService.handleUpdateMenu(menuId, updateMenuRequest);

        ChefResponse chefResponse = chefMapper.toChefResponse(chef);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("menu updated successfully")
                        .success(true)
                        .data(Map.of("menu", menuResponse, "chef", chefResponse))
                        .build()
        );
    }

    @DeleteMapping("/{menuId}")
    public ResponseEntity<ApiResponse> deleteMenu(@PathVariable("menuId") UUID menuId) {
        menuService.handleDeleteMenu(menuId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.builder()
                        .message("menu deleted successfully")
                        .success(true)
                        .build()
        );
    }
}
