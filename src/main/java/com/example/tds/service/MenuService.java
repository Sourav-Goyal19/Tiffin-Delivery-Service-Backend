package com.example.tds.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import com.example.tds.entity.ChefEntity;
import com.example.tds.entity.MenuEntity;
import com.example.tds.mapper.MenuMapper;
import org.springframework.stereotype.Service;
import com.example.tds.repository.MenuRepository;
import com.example.tds.dto.responses.MenuResponse;
import com.example.tds.dto.requests.menu.CreateMenuRequest;
import com.example.tds.dto.requests.menu.UpdateMenuRequest;
import com.example.tds.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuService {
    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;

    public MenuResponse handleMenuCreation(ChefEntity chef, CreateMenuRequest createMenuRequest){
        MenuEntity menu = menuMapper.toMenuEntity(createMenuRequest);
        menu.setChef(chef);

        MenuEntity createdMenu = menuRepository.save(menu);

        return menuMapper.toMenuResponse(createdMenu);
    }

    public List<MenuResponse> handleMultipleMenuCreation(ChefEntity chef, List<CreateMenuRequest> createMenuRequests){
        List<MenuEntity> menuEntities = menuMapper.toMenuEntities(createMenuRequests).stream().peek(k -> k.setChef(chef)).toList();

        List<MenuEntity> createdMenus = menuRepository.saveAll(menuEntities);

        return menuMapper.toMenuResponse(createdMenus);
    }

    public MenuResponse handleGetMenu(UUID menuId){
        MenuEntity existingMenu = findMenuById(menuId);

        return menuMapper.toMenuResponse(existingMenu);
    }

    public List<MenuResponse> handleGetAllMenus(UUID chefId){
        List<MenuEntity> menus = menuRepository.findAllByChefChefId(chefId)
                .orElseThrow(()->new ResourceNotFoundException("No menus found"));

        return menuMapper.toMenuResponse(menus);
    }

    public MenuResponse handleUpdateMenu(UUID menuId, UpdateMenuRequest updateMenuRequest){
        MenuEntity existingMenu = findMenuById(menuId);

        existingMenu.setItems(updateMenuRequest.getItems());
        existingMenu.setWeekDay(updateMenuRequest.getWeekDay());

        MenuEntity updatedMenu = menuRepository.save(existingMenu);

        return menuMapper.toMenuResponse(updatedMenu);
    }

    public void handleDeleteMenu(UUID menuId){
        MenuEntity existingMenu = findMenuById(menuId);

        menuRepository.delete(existingMenu);
    }

    private MenuEntity findMenuById(UUID menuId){
        return menuRepository.findByMenuId(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found"));
    }
}
