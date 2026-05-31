package com.example.tds.service;

import lombok.extern.slf4j.Slf4j;
import com.example.tds.enums.WeekDay;
import com.example.tds.enums.MealType;
import lombok.RequiredArgsConstructor;
import com.example.tds.entity.ChefEntity;
import com.example.tds.entity.MenuEntity;
import com.example.tds.mapper.MenuMapper;
import org.springframework.stereotype.Service;
import com.example.tds.repository.MenuRepository;
import com.example.tds.dto.responses.MenuResponse;
import org.springframework.web.multipart.MultipartFile;
import com.example.tds.dto.requests.menu.CreateMenuRequest;
import com.example.tds.dto.requests.menu.UpdateMenuRequest;
import com.example.tds.exception.ResourceNotFoundException;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MenuService {
    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;
    private final StorageService storageService;

    public MenuResponse handleMenuCreation(ChefEntity chef, CreateMenuRequest createMenuRequest, MultipartFile thumbnail) {
        MenuEntity menu = menuMapper.toMenuEntity(createMenuRequest);
        menu.setChef(chef);

        MenuEntity existingMealMenu = menuRepository.findByChefChefIdAndMealTypeAndWeekDayAndIsActiveTrue(
                    chef.getChefId(),
                    menu.getMealType(),
                    menu.getWeekDay()
                )
                .orElse(null);

        boolean isActive = existingMealMenu == null;

        menu.setIsActive(isActive);

        if(thumbnail != null && !thumbnail.isEmpty()) {
            String bucketName = "items-thumbnail";

            String fileName = UUID.randomUUID() + "." + thumbnail.getOriginalFilename();
            storageService.uploadFile(bucketName, fileName, thumbnail, true);

            String fileUrl = storageService.getPublicUrl(bucketName, fileName);

            menu.setThumbnailUrl(fileUrl);
        }

        MenuEntity createdMenu = menuRepository.save(menu);

        return menuMapper.toMenuResponse(createdMenu);
    }

    public List<MenuResponse> handleMultipleMenuCreation(ChefEntity chef, List<CreateMenuRequest> createMenuRequests){

        List<MealType> mealTypes = new ArrayList<>(Arrays.asList(MealType.values()));
        List<WeekDay> weekDays = new ArrayList<>(Arrays.asList(WeekDay.values()));

        HashMap<String, Boolean> isActiveMap = new HashMap<>();

        // :-: Determining whether any meal type with any weekday has any active menu or not :-:
        for(MealType mealType : mealTypes){
            for(WeekDay weekDay : weekDays){
                MenuEntity existingMealMenu = menuRepository.findByChefChefIdAndMealTypeAndWeekDayAndIsActiveTrue(
                                chef.getChefId(),
                                mealType,
                                weekDay
                        )
                        .orElse(null);

                isActiveMap.put(String.join(mealType.name(),  weekDay.name()), existingMealMenu != null);
            }
        }

        // :-: If any menu is already active, then don't make anyone active with the same meal type and the weekday :-:
        List<MenuEntity> menuEntities = menuMapper.toMenuEntities(createMenuRequests).stream().peek(k ->{
            k.setChef(chef);

            MealType currentMealType = k.getMealType();
            WeekDay currentWeekDay = k.getWeekDay();

            String key = String.join(currentMealType.name(), currentWeekDay.name());
            Boolean isActive = isActiveMap.get(key);

            k.setIsActive(!isActive);

            if(!isActive) isActiveMap.put(key, true);
        }).toList();

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

    public MenuResponse handleUpdateMenu(UUID menuId, UpdateMenuRequest updateMenuRequest, MultipartFile thumbnail) {
        MenuEntity existingMenu = findMenuById(menuId);
        MenuEntity updatedMenu = menuMapper.toMenuEntity(updateMenuRequest);

        Boolean isActive = updatedMenu.getIsActive();

        MenuEntity currentlyActive = menuRepository.findByChefChefIdAndMealTypeAndWeekDayAndIsActiveTrue(
                existingMenu.getChef().getChefId(),
                updatedMenu.getMealType(),
                updatedMenu.getWeekDay()
        ).orElse(null);

        if(currentlyActive != null && isActive){
            currentlyActive.setIsActive(false);
            menuRepository.save(currentlyActive);
        }

        if(thumbnail != null && !thumbnail.isEmpty()) {
            String bucketName = "items-thumbnail";

            String fileName = UUID.randomUUID() + "." + thumbnail.getOriginalFilename();
            storageService.uploadFile(bucketName, fileName, thumbnail, true);

            String fileUrl = storageService.getPublicUrl(bucketName, fileName);
            updatedMenu.setThumbnailUrl(fileUrl);
        }

        existingMenu.setItems(updateMenuRequest.getItems());
        existingMenu.setWeekDay(updateMenuRequest.getWeekDay());
        existingMenu.setIsActive(isActive);
        existingMenu.setThumbnailUrl(updatedMenu.getThumbnailUrl());

        updatedMenu = menuRepository.save(existingMenu);

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
