package com.example.ourgarden.service.impl;

import com.example.ourgarden.model.binding.DayAddStockBindingModel;
import com.example.ourgarden.model.entity.DayEntity;
import com.example.ourgarden.model.entity.OrderEntity;
import com.example.ourgarden.model.entity.enums.ProductNameEnum;
import com.example.ourgarden.model.service.DayAddStockServiceModel;
import com.example.ourgarden.model.view.DayViewModel;
import com.example.ourgarden.repository.DayRepository;
import com.example.ourgarden.service.DayService;
import com.example.ourgarden.service.ProductService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DayServiceImpl implements DayService {
    private final DayRepository dayRepository;
    private final ModelMapper modelMapper;
    private final ProductService productService;

    public DayServiceImpl(DayRepository dayRepository, ModelMapper modelMapper, ProductService productService) {
        this.dayRepository = dayRepository;
        this.modelMapper = modelMapper;
        this.productService = productService;
    }

    @Override
    public void addStock(DayAddStockServiceModel dayAddStockServiceModel) {
        DayEntity dayEntity = new DayEntity();
        dayEntity.setDate(dayAddStockServiceModel.getDate());
        dayEntity.setProduct(productService.findByProductNameEnum(dayAddStockServiceModel.getProductNameEnum()).orElse(null));
        dayEntity.setQuantity(dayAddStockServiceModel.getQuantity());
        dayEntity.setReservedQuantity(BigDecimal.valueOf(0));
        calculateVariables(dayEntity);
    }

    @Override
    public DayEntity findByDateAndProduct(LocalDate date, ProductNameEnum productNameEnum) {
        return dayRepository.findByDateAndProduct_ProductNameEnum(date, productNameEnum);
    }

    @Override
    public DayViewModel findByID(Long id) {
        return dayRepository.findById(id).map(dayEntity -> modelMapper.map(dayEntity,DayViewModel.class)).orElse(null);
    }

    @Override
    public void calculateVariables(DayEntity dayEntity) {
        BigDecimal reservedQuantity = dayEntity.getOrders().stream()
                .map(OrderEntity::getQuantity)
                .collect(Collectors.toList())
                .stream().reduce(BigDecimal.ZERO,BigDecimal::add);
        dayEntity.setReservedQuantity(reservedQuantity);
        dayEntity.setMaxQuantity(dayEntity.getQuantity().subtract(dayEntity.getReservedQuantity()));
        dayEntity.setMinQuantity(dayEntity.getQuantity().multiply(BigDecimal.valueOf(0.05)));
        dayEntity.setActive(dayEntity.getMaxQuantity().compareTo(dayEntity.getMinQuantity()) > 0);
        dayRepository.save(dayEntity);
    }

    @Override
    public List<DayViewModel> findByDate(LocalDate now) {
        return dayRepository.findByDateAfter(now)
                .stream().map(day -> modelMapper.map(day,DayViewModel.class)).collect(Collectors.toList());
    }

    @Override
    public void addOrder(OrderEntity order) {
        DayEntity day = order.getDayEntity();
        day.addOrder(order);
        calculateVariables(day);
    }

    @Override
    public List<DayViewModel> findByDateAndActive(LocalDate now) {
        return dayRepository.findByDateAfterAndActive(now,true)
                .stream().map(day -> modelMapper.map(day,DayViewModel.class)).collect(Collectors.toList());
    }

    @Override
    public void removeOrder(OrderEntity order) {
        DayEntity day = order.getDayEntity();
        day.getOrders().remove(order);
        calculateVariables(day);
    }

    @Override
    public void blockDayProduct(Long id) {
        DayEntity dayEntity = dayRepository.findById(id).orElse(null);
        assert dayEntity != null;
        dayEntity.setQuantity(dayEntity.getReservedQuantity());
        save(dayEntity);
    }

    @Override
    public void addOrCreate(DayAddStockBindingModel dayAddStockBindingModel) {
        DayEntity dayEntity = findByDateAndProduct(dayAddStockBindingModel.getDate(), dayAddStockBindingModel.getProductNameEnum());
        if (dayEntity != null){
            dayEntity.setQuantity(dayEntity.getQuantity().add(dayAddStockBindingModel.getQuantity()));
            save(dayEntity);
        }else {
            addStock(modelMapper.map(dayAddStockBindingModel, DayAddStockServiceModel.class));
        }
    }

    @Override
    public void save(DayEntity dayEntity) {
        calculateVariables(dayEntity);
    }
}
