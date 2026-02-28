package de.klg71.solarman_sensor.battery


fun bitRead(inByte: Int, bitIndex: Int): Boolean =
    inByte == ((1 shl bitIndex) and (1 shl bitIndex))

fun parseErrorCodes(bytes: List<Int>): List<String> {
    return buildList {
        if (bitRead(bytes[4], 1)) {
            add("Cell Voltage high l2")
        }
        if (bitRead(bytes[4], 0)) {
            add("Cell Voltage high l1")
        }
        if (bitRead(bytes[4], 3)) {
            add("Cell Voltage low l2")
        }
        if (bitRead(bytes[4], 2)) {
            add("Cell voltage low l1")
        }
        if (bitRead(bytes[4], 5)) {
            add("Sum Voltage high l2")
        }
        if (bitRead(bytes[4], 4)) {
            add("Sum Voltage high l1")
        }
        if (bitRead(bytes[4], 7)) {
            add("Sum Voltage low l2")
        }
        if (bitRead(bytes[4], 6)) {
            add("Sum Voltage low l1")
        }
        if (bitRead(bytes[5], 1)) {
            add("Charge Temperature high l2")
        }
        if (bitRead(bytes[5], 0)) {
            add("Charge Temperature high l1")
        }
        if (bitRead(bytes[5], 3)) {
            add("Charge Temperature low l2")
        }
        if (bitRead(bytes[5], 2)) {
            add("Charge Temperature low l1")
        }
        if (bitRead(bytes[5], 5)) {
            add("Discharge Temperature high l2")
        }
        if (bitRead(bytes[5], 4)) {
            add("Discharge Temperature high l1")
        }
        if (bitRead(bytes[5], 7)) {
            add("Discharge Temperature low l2")
        }
        if (bitRead(bytes[5], 6)) {
            add("Discharge Temperature low l1")
        }
        if (bitRead(bytes[6], 1)) {
            add("Charge OverCurrent l1")
        }
        if (bitRead(bytes[6], 0)) {
            add("Charge OverCurrent l1")
        }
        if (bitRead(bytes[6], 3)) {
            add("Discharge OverCurrent l2")
        }
        if (bitRead(bytes[6], 2)) {
            add("Discharge OverCurrent l1")
        }
        if (bitRead(bytes[6], 5)) {
            add("SOC high l2")
        }
        if (bitRead(bytes[6], 4)) {
            add("SOC high l1")
        }
        if (bitRead(bytes[6], 7)) {
            add("SOC low l2")
        }
        if (bitRead(bytes[6], 6)) {
            add("SOC low l1")
        }
        if (bitRead(bytes[7], 1)) {
            add("Cell Diff oltage l2")
        }
        if (bitRead(bytes[7], 0)) {
            add("Cell Diff Voltage l1")
        }
        if (bitRead(bytes[7], 3)) {
            add("Diff Temperature l2")
        }
        if (bitRead(bytes[7], 2)) {
            add("Diff Temperature l1")
        }
        if (bitRead(bytes[8], 0)) {
            add("Charge MOS Temperature high")
        }
        if (bitRead(bytes[8], 1)) {
            add("Discharge MOS Temperature high")
        }
        if (bitRead(bytes[8], 2)) {
            add("Charge MOS Temperature sensor err")
        }
        if (bitRead(bytes[8], 3)) {
            add("Discharge MOS Temperature sensor err")
        }
        if (bitRead(bytes[8], 4)) {
            add("Charge MOS adh err")
        }
        if (bitRead(bytes[8], 5)) {
            add("Discharge MOS adh err")
        }
        if (bitRead(bytes[8], 6)) {
            add("Charge MOS open circuit err")
        }
        if (bitRead(bytes[8], 7)) {
            add("Discharge MOS open circuit err")
        }
        if (bitRead(bytes[9], 0)) {
            add("AFE collect chip err")
        }
        if (bitRead(bytes[9], 1)) {
            add("Volt collect dropped")
        }
        if (bitRead(bytes[9], 2)) {
            add("Cell tempt sensor err")
        }
        if (bitRead(bytes[9], 3)) {
            add("EEPROM err")
        }
        if (bitRead(bytes[9], 4)) {
            add("RTC err")
        }
        if (bitRead(bytes[9], 5)) {
            add("Precharge fail")
        }
        if (bitRead(bytes[9], 6)) {
            add("Comm fail")
        }
        if (bitRead(bytes[9], 7)) {
            add("Int comm fail")
        }
        if (bitRead(bytes[10], 0)) {
            add("Current module fault")
        }
        if (bitRead(bytes[10], 1)) {
            add("S Volt detect fault")
        }
        if (bitRead(bytes[10], 2)) {
            add("S Current protect fault")
        }
        if (bitRead(bytes[10], 3)) {
            add("L Voltage forb chg fault")
        }
    }
}