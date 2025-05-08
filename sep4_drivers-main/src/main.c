#include "wifi.h"
#include "uart.h"
#include "hc_sr04.h"
#include "display.h"
#include "dht11.h"
#include "pir.h"
#include <util/delay.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <avr/interrupt.h>
#include <stdio.h>

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
    }
}

int main()
{
    char prompt_text[] = "Starting sensor measurements...\n";

    uart_init(USART_0, 9600, console_rx);
    wifi_init();
    hc_sr04_init();
    display_init();
    dht11_init();
    pir_init(pir_motion_detected);

    sei();

    wifi_command_join_AP("INTERNET", "INTERNETPASS");
    wifi_command_create_TCP_connection("IP", 23, NULL, NULL);

    uart_send_string_blocking(USART_0, prompt_text);

    char send_buffer[128];

    while (1)
    {
        uint16_t distance = hc_sr04_takeMeasurement() / 58; // SKAL JUSTERES!
        display_int(distance);                         

        uint8_t hum_int = 0, hum_dec = 0, temp_int = 0, temp_dec = 0;
        DHT11_ERROR_MESSAGE_t dht_status = dht11_get(&hum_int, &hum_dec, &temp_int, &temp_dec);

        if (dht_status == DHT11_OK)
        {
            sprintf(send_buffer, "Distance: %u cm, Temp: %d.%d C, Humidity: %d.%d %%, Motion: %s\n",
                    distance, temp_int, temp_dec, hum_int, hum_dec,
                    motion_detected ? "Yes" : "No");
        }
        else
        {
            sprintf(send_buffer, "Distance: %u cm, DHT11 sensor error!, Motion: %s\n",
                distance, motion_detected ? "Yes" : "No");
        }

        wifi_command_TCP_transmit((uint8_t*)send_buffer, strlen(send_buffer));
        uart_send_string_blocking(USART_0, send_buffer);

        motion_detected = false;

        _delay_ms(10000); 
    }

    return 0;
}