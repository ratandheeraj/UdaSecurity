package com.udacity.security.service;

import com.udacity.image.service.ImageService;
import com.udacity.security.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;
    private Sensor sensor;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private ImageService imageService;

    private Set<Sensor> getAllSensors(boolean status) {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            sensors.add(new Sensor(UUID.randomUUID().toString(), SensorType.DOOR));
        }
        sensors.forEach(sensor -> sensor.setActive(status));

        return sensors;
    }
    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
    }

    @Test
    @DisplayName("1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.")
    void test1() {

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    @DisplayName("2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.")
    void test2() {

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
//    @Test
//    DisplayName("3. If pending alarm and all sensors are inactive, return to no alarm state.")
//    void test3(){
//        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
//        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
//        sensor.setActive(false);
//        securityService.changeSensorActivationStatus(sensor,false);
//
//        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
//    }

    @RepeatedTest(value = 2)
    @DisplayName("4. If alarm is active, change in sensor state should not affect the alarm state")
    void test4(RepetitionInfo repetitionInfo) {
        lenient().when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, repetitionInfo.getCurrentRepetition() == 1);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }


//    @Test
 //   @DisplayName("5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.");
//    void test5(){
//        sensor.setActive(true);
//        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
//
//        securityService.changeSensorActivationStatus(sensor, true);
//
//        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
//    }

//    @ParameterizedTest
//    DisplayName("6. If a sensor is deactivated while already inactive, make no changes to the alarm state.")
//    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
//    void test6(AlarmStatus status) {
//        when(securityRepository.getAlarmStatus()).thenReturn(status);
//        sensor.setActive(false);
//        securityService.changeSensorActivationStatus(sensor, false);
//
//        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
//    }

    @Test
    @DisplayName("7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.")
    void test7() {
        BufferedImage catImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(catImage);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
    @Test
    @DisplayName("8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.")
    void test8(){
        Set<Sensor> sensors = getAllSensors(false);
        lenient().when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    @DisplayName("9. If the system is disarmed, set the status to no alarm.")
    void test9(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


//    @DisplayName("10. If the system is armed, reset all sensors to inactive.")
//    @ParameterizedTest
//    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
//    void test10(ArmingStatus status) {
//        Set<Sensor> sensors = getAllSensors(true);
//        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
//        when(securityRepository.getSensors()).thenReturn(sensors);
//        securityService.setArmingStatus(status);
//
//        securityService.getSensors().forEach(sensor -> {
//            assertFalse(sensor.getActive());
//        });
//    }

    @Test
    @DisplayName("11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.")
    void test11() {
        BufferedImage catImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}
