package com.app.appointment.service;

import com.app.appointment.dto.SlotRequest;
import com.app.appointment.dto.SlotResponse;

import java.time.LocalDate;
import java.util.List;

public interface SlotService {
    SlotResponse createSlot(SlotRequest request);
    List<SlotResponse> getAvailableSlots(Long providerId);
    List<SlotResponse> getAllSlotsOfProvider(Long providerId);
    List<SlotResponse> getPublicAvailableSlots(LocalDate date, String specialization, String search);
    void deleteSlot(Long slotId, Long providerId);
    boolean isProviderAvailableOn(Long providerId, LocalDate date);
}
