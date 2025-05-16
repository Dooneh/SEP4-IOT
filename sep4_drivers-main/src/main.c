#include "wifi.h"
#include "uart.h"
#include "hc_sr04.h"
#include "display.h"
#include "dht11.h"
#include "light.h"
#include "pir.h"
#include "water_pump.h"
#include <util/delay.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <avr/interrupt.h>
#include <stdio.h>

static uint8_t timer_counter = 0;

static uint8_t _buff[100];
static uint8_t _index = 0;
volatile static bool _done = false;
volatile static bool motion_detected = false;

void pir_motion_detected(void) {
    motion_detected = true;
    uart_send_string_blocking(USART_0, "Motion detected!\n");
}

void console_rx(uint8_t _rx)
{
    uart_send_blocking(USART_0, _rx);
    if (('\r' != _rx) && ('\n' != _rx))
    {
        if (_index < 100 - 1)
        {
            _buff[_index++] = _rx;
        }
    }
    else
    {
        _buff[_index] = '\0';
        _index = 0;
        _done = true;
        uart_send_blocking(USART_0, '\n');
        
        if (strcmp((char*)_buff, "pump on") == 0) {
            water_pump_start();
            uart_send_string_blocking(USART_0, "Water pump started\n");
        }
        else if (strcmp((char*)_buff, "pump off") == 0) {
            water_pump_stop();
            uart_send_string_blocking(USART_0, "Water pump stopped\n");
        }
        else if (strncmp((char*)_buff, "pump run ", 9) == 0) {
            uint16_t seconds = atoi((char*)_buff + 9);
            if (seconds > 0) {
                water_pump_run_for(seconds);
                char response[40];
                sprintf(response, "Water pump running for %u seconds\n", seconds);
                uart_send_string_blocking(USART_0, response);
            }
        }
    }
}

uint8_t convert_light_to_percent(uint16_t raw_value)
{
    uint8_t percentage = 100 - ((raw_value * 100) / 1023);
    
    if (percentage > 100) percentage = 100;
    
    return percentage;
}

int main()
{
    char prompt_text[] = "Starting sensor measurements...\n";

    uart_init(USART_0, 9600, console_rx);
    wifi_init();
    hc_sr04_init();
    display_init();
    dht11_init();
    light_init();
    pir_init(pir_motion_detected);
    water_pump_init();

    sei();

    wifi_command_join_AP("INTERNET", "INTERNETPASS");
    wifi_command_create_TCP_connection("IP", 23, NULL, NULL);

    uart_send_string_blocking(USART_0, prompt_text);
    uart_send_string_blocking(USART_0, "Water pump control commands available:\n");
    uart_send_string_blocking(USART_0, "  'pump on' - Start the water pump\n");
    uart_send_string_blocking(USART_0, "  'pump off' - Stop the water pump\n");
    uart_send_string_blocking(USART_0, "  'pump run X' - Run the pump for X seconds\n");

    char send_buffer[128];
    bool pump_state = false;

    while (1)
    {
        pump_state = !pump_state;
        
        if (pump_state) {
            water_pump_start();
            uart_send_string_blocking(USART_0, "Water pump: ON\n");
        } else {
            water_pump_stop();
            uart_send_string_blocking(USART_0, "Water pump: OFF\n");
        }
        
        display_int(pump_state ? 1 : 0);
        
        sprintf(send_buffer, "Water pump state: %s\n", pump_state ? "ON" : "OFF");
        wifi_command_TCP_transmit((uint8_t*)send_buffer, strlen(send_buffer));
        
        _delay_ms(2000); // TODO: Skal ændres til at køre afhængig af soil humidity eller manuelt fra web interface
    }

    while (1)
    {
        uint16_t distance = hc_sr04_takeMeasurement() / 58; // SKAL JUSTERES!
        display_int(distance);
        uint16_t light_raw = light_read();

        uint8_t light_percent = convert_light_to_percent(light_raw);

        uint8_t hum_int = 0, hum_dec = 0, temp_int = 0, temp_dec = 0;
        DHT11_ERROR_MESSAGE_t dht_status = dht11_get(&hum_int, &hum_dec, &temp_int, &temp_dec);

        if (dht_status == DHT11_OK)
        {
            sprintf(send_buffer, "Distance: %u cm, Temp: %d.%d C, Humidity: %d.%d, Light: %u%% (raw: %u), Motion: %s, Pump: %s\n",
                    distance, temp_int, temp_dec, hum_int, hum_dec, light_percent, light_raw, 
                    motion_detected ? "Yes" : "No", water_pump_is_running() ? "ON" : "OFF");
        }
        else
        {
            sprintf(send_buffer, "Distance: %u cm, DHT11 sensor error!, Light: %u%% (raw: %u), Motion: %s, Pump: %s\n", 
                    distance, light_percent, light_raw, motion_detected ? "Yes" : "No", water_pump_is_running() ? "ON" : "OFF");
        }

        wifi_command_TCP_transmit((uint8_t*)send_buffer, strlen(send_buffer));
        uart_send_string_blocking(USART_0, send_buffer);

        motion_detected = false;

        for (timer_counter = 0; timer_counter < 10; timer_counter++) {
            water_pump_update();
            _delay_ms(1000);
        }
    }

    return 0;
}
