package com.udacity.security.service;

import com.udacity.image.service.ImageService;
import com.udacity.security.application.StatusListener;
import com.udacity.security.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {
    private SecurityService securityService;
    private Sensor sensor;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private ImageService imageService;
    @Mock
    private StatusListener statusListener;

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
        sensor = new Sensor(UUID.randomUUID().toString(), SensorType.WINDOW);
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
    @Test
    @DisplayName("3. If pending alarm and all sensors are inactive, return to no alarm state.")
    void test3(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatusWithSensor(sensor);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @RepeatedTest(value = 2)
    @DisplayName("4. If alarm is active, change in sensor state should not affect the alarm state")
    void test4(RepetitionInfo repetitionInfo) {
        lenient().when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, repetitionInfo.getCurrentRepetition() == 1);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }


    @Test
    @DisplayName("5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.")
    void test5(){
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @DisplayName("6. If a sensor is deactivated while already inactive, make no changes to the alarm state.")
    @EnumSource(value = AlarmStatus.class, names = {"ALARM","NO_ALARM", "PENDING_ALARM"})
    void test6(AlarmStatus status) {
        sensor.setActive(false);
        lenient().when(securityRepository.getAlarmStatus()).thenReturn(status);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    @DisplayName("7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.")
    void test7() {
        BufferedImage bufferedImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
    @Test
    @DisplayName("8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.")
    void test8(){
        Set<Sensor> sensors = getAllSensors(false);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        lenient().when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    @DisplayName("9. If the system is disarmed, set the status to no alarm.")
    void test9(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    @DisplayName("10. If the system is armed, reset all sensors to inactive.")
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void test10(ArmingStatus status) {
        Set<Sensor> sensors = getAllSensors(true);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.setArmingStatus(status);

        securityService.getSensors().forEach(sensor -> {
            Assertions.assertFalse(sensor.getActive());
        });
    }

    @Test
    @DisplayName("11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.")
    void test11() {
        BufferedImage bufferedImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }


    /**
     * Additional tests for coverage
     */

    @Test
    @DisplayName("Add sensors")
    void test12() {
        securityService.addSensor(sensor);
        securityService.addSensor(sensor);
        verify(securityRepository, times(2)).addSensor(sensor);
    }
    @Test
    @DisplayName("Remove sensors")
    void test13() {
        securityService.removeSensor(sensor);
        securityService.removeSensor(sensor);
        verify(securityRepository, times(2)).removeSensor(sensor);
    }
    @Test
    @DisplayName("Add status listener")
    void test14() {
        securityService.addStatusListener(statusListener);
        securityService.addStatusListener(statusListener);
    }
    @Test
    @DisplayName("Remove status listener")
    void test15() {
        securityService.removeStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    @DisplayName("If alarm state and system  are disarmed change status to pending")
    void test16() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatusWithSensor(sensor);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM"})
    @DisplayName("If system disarmed and sensor is activated no change to the arming state")
    void test17(AlarmStatus status) {
        when(securityRepository.getAlarmStatus()).thenReturn(status);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setArmingStatus(ArmingStatus.DISARMED);
    }

}
