package com.app.appointment.service;

import com.app.appointment.dto.AppointmentRequest;
import com.app.appointment.dto.AppointmentResponse;
import com.app.appointment.dto.RescheduleAppointmentRequest;

import java.util.List;

public interface AppointmentService {
    AppointmentResponse bookSlot(Long slotId, AppointmentRequest request);
    AppointmentResponse cancelAppointment(Long appointmentId, Long userId);
    AppointmentResponse rescheduleAppointment(Long appointmentId, Long userId, RescheduleAppointmentRequest request);
    AppointmentResponse completeAppointment(Long appointmentId, Long providerId);
    List<AppointmentResponse> getMyAppointments(Long userId);
    List<AppointmentResponse> getProviderAppointments(Long providerId);
}
