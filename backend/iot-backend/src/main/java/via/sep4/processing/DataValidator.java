package via.sep4.processing;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.validation.ValidationException;

@Component
public class DataValidator {
    private static final Logger logger = LoggerFactory.getLogger(DataValidator.class);

    public enum ValidationResult {
        VALIDATION_SUCCESS,
        VALIDATION_ERROR_LUFT_TEMPERATUR,
        VALIDATION_ERROR_LUFTFUGTIGHED,
        VALIDATION_ERROR_JORD_FUGTIGHED,
        VALIDATION_ERROR_LYS_HØJESTE_INTENSITET,
        VALIDATION_ERROR_LYS_LAVESTE_INTENSITET,
        VALIDATION_ERROR_LYS_INDSTILLING,
        VALIDATION_ERROR_LYS_GENNEMSNIT,
        VALIDATION_ERROR_AFSTAND_TIL_HØJDE,
        VALIDATION_ERROR_VAND_TID_FRA_SIDSTE,
        VALIDATION_ERROR_VAND_MÆNGDE,
        VALIDATION_ERROR_VAND_FREKVENS,
        VALIDATION_ERROR_TIDSSTEMPEL,
        VALIDATION_ERROR_GENERAL
    }

    private int errorRow = 0;
    private List<String> validationErrors = new ArrayList<>();

    public ValidationResult validate(DataConverter converter) {
        if (converter == null || converter.getData().isEmpty() || converter.getRows() <= 0
                || converter.getCols() <= 0) {
            return ValidationResult.VALIDATION_ERROR_GENERAL;
        }

        int luftTempIdx = findColumnIndex(converter, "Luft_temperatur");
        int luftfugtighedIdx = findColumnIndex(converter, "Luftfugtighed");
        int jordFugtighedIdx = findColumnIndex(converter, "Jord_fugtighed");
        int lysTypeIdx = findColumnIndex(converter, "Lys_type");
        int lysHøjesteIdx = findColumnIndex(converter, "Lys_højeste_intensitet");
        int lysLavesteIdx = findColumnIndex(converter, "Lys_laveste_intensitet");
        int lysIndstillingIdx = findColumnIndex(converter, "Lys_indstilling");
        int lysGennemsnitIdx = findColumnIndex(converter, "Lys_gennemsnit");
        int afstandTilHøjdeIdx = findColumnIndex(converter, "Afstand_til_Højde");
        int vandTidFraSidsteIdx = findColumnIndex(converter, "Vand_tid_fra_sidste");
        int vandMængdeIdx = findColumnIndex(converter, "Vand_mængde");
        int vandFrekvensIdx = findColumnIndex(converter, "Vand_frekvens");
        int tidsstempelIdx = findColumnIndex(converter, "Tidsstempel");

        if (luftTempIdx == -1 || luftfugtighedIdx == -1 || jordFugtighedIdx == -1) {
            return ValidationResult.VALIDATION_ERROR_GENERAL;
        }

        for (int row = 0; row < converter.getRows(); row++) {
            errorRow = row;
            Map<String, String> rowData = converter.getData().get(row);

            Float tempValue = parseFloat(rowData.get(converter.getHeaders().get(luftTempIdx)));
            if (tempValue == null || tempValue < 15.0f || tempValue > 40.0f) {
                return ValidationResult.VALIDATION_ERROR_LUFT_TEMPERATUR;
            }

            Integer luftfugtighedValue = parseInt(rowData.get(converter.getHeaders().get(luftfugtighedIdx)));
            if (luftfugtighedValue == null || luftfugtighedValue < 0 || luftfugtighedValue > 100) {
                return ValidationResult.VALIDATION_ERROR_LUFTFUGTIGHED;
            }

            Integer jordFugtighedValue = parseInt(rowData.get(converter.getHeaders().get(jordFugtighedIdx)));
            if (jordFugtighedValue == null || jordFugtighedValue < 0 || jordFugtighedValue > 100) {
                return ValidationResult.VALIDATION_ERROR_JORD_FUGTIGHED;
            }

            if (lysHøjesteIdx != -1) {
                Integer lysHøjesteValue = parseInt(rowData.get(converter.getHeaders().get(lysHøjesteIdx)));
                if (lysHøjesteValue == null || lysHøjesteValue <= 0) {
                    return ValidationResult.VALIDATION_ERROR_LYS_HØJESTE_INTENSITET;
                }

                if (lysLavesteIdx != -1) {
                    Integer lysLavesteValue = parseInt(rowData.get(converter.getHeaders().get(lysLavesteIdx)));
                    if (lysLavesteValue == null || lysLavesteValue < 0) {
                        return ValidationResult.VALIDATION_ERROR_LYS_LAVESTE_INTENSITET;
                    }

                    if (lysHøjesteValue <= lysLavesteValue) {
                        return ValidationResult.VALIDATION_ERROR_LYS_HØJESTE_INTENSITET;
                    }
                }
            }

            if (lysIndstillingIdx != -1) {
                Integer lysIndstillingValue = parseInt(rowData.get(converter.getHeaders().get(lysIndstillingIdx)));
                if (lysIndstillingValue == null || lysIndstillingValue < 0 || lysIndstillingValue > 10) {
                    return ValidationResult.VALIDATION_ERROR_LYS_INDSTILLING;
                }
            }

            if (afstandTilHøjdeIdx != -1) {
                Integer afstandValue = parseInt(rowData.get(converter.getHeaders().get(afstandTilHøjdeIdx)));
                if (afstandValue == null || afstandValue <= 0) {
                    return ValidationResult.VALIDATION_ERROR_AFSTAND_TIL_HØJDE;
                }
            }

            if (vandTidFraSidsteIdx != -1) {
                Integer vandTidValue = parseInt(rowData.get(converter.getHeaders().get(vandTidFraSidsteIdx)));
                if (vandTidValue == null || vandTidValue < 0) {
                    return ValidationResult.VALIDATION_ERROR_VAND_TID_FRA_SIDSTE;
                }
            }

            if (vandMængdeIdx != -1) {
                Integer vandMængdeValue = parseInt(rowData.get(converter.getHeaders().get(vandMængdeIdx)));
                if (vandMængdeValue == null || vandMængdeValue <= 0) {
                    return ValidationResult.VALIDATION_ERROR_VAND_MÆNGDE;
                }
            }

            if (vandFrekvensIdx != -1) {
                Integer vandFrekvensValue = parseInt(rowData.get(converter.getHeaders().get(vandFrekvensIdx)));
                if (vandFrekvensValue == null || vandFrekvensValue <= 0) {
                    return ValidationResult.VALIDATION_ERROR_VAND_FREKVENS;
                }
            }

            if (tidsstempelIdx != -1) {
                if (!isValidTimestamp(rowData.get(converter.getHeaders().get(tidsstempelIdx)))) {
                    return ValidationResult.VALIDATION_ERROR_TIDSSTEMPEL;
                }
            }
        }

        return ValidationResult.VALIDATION_SUCCESS;
    }

    public String getErrorMessage(ValidationResult result) {
        switch (result) {
            case VALIDATION_SUCCESS:
                return "Validation successful";
            case VALIDATION_ERROR_LUFT_TEMPERATUR:
                return String.format(
                        "Validation failed: Luft_temperatur must be a float between 15°C and 40°C at row %d",
                        errorRow + 1);
            case VALIDATION_ERROR_LUFTFUGTIGHED:
                return String.format(
                        "Validation failed: Luftfugtighed must be an integer between 0%% and 100%% at row %d",
                        errorRow + 1);
            case VALIDATION_ERROR_JORD_FUGTIGHED:
                return String.format(
                        "Validation failed: Jord_fugtighed must be an integer between 0%% and 100%% at row %d",
                        errorRow + 1);
            case VALIDATION_ERROR_LYS_HØJESTE_INTENSITET:
                return String.format(
                        "Validation failed: Lys_højeste_intensitet must be a positive integer greater than Lys_laveste_intensitet at row %d",
                        errorRow + 1);
            case VALIDATION_ERROR_LYS_LAVESTE_INTENSITET:
                return String.format(
                        "Validation failed: Lys_laveste_intensitet must be a non-negative integer at row %d",
                        errorRow + 1);
            case VALIDATION_ERROR_LYS_INDSTILLING:
                return String.format("Validation failed: Lys_indstilling must be an integer between 0 and 10 at row %d",
                        errorRow + 1);
            case VALIDATION_ERROR_LYS_GENNEMSNIT:
                return String.format("Validation failed: Lys_gennemsnit validation error at row %d", errorRow + 1);
            case VALIDATION_ERROR_AFSTAND_TIL_HØJDE:
                return String.format("Validation failed: Afstand_til_Højde must be a positive integer at row %d",
                        errorRow + 1);
            case VALIDATION_ERROR_VAND_TID_FRA_SIDSTE:
                return String.format("Validation failed: Vand_tid_fra_sidste must be a non-negative integer at row %d",
                        errorRow + 1);
            case VALIDATION_ERROR_VAND_MÆNGDE:
                return String.format("Validation failed: Vand_mængde must be a positive integer at row %d",
                        errorRow + 1);
            case VALIDATION_ERROR_VAND_FREKVENS:
                return String.format("Validation failed: Vand_frekvens must be a positive integer at row %d",
                        errorRow + 1);
            case VALIDATION_ERROR_TIDSSTEMPEL:
                return String.format("Validation failed: Tidsstempel must be in YYYY-MM-DDThh:mm:ss format at row %d",
                        errorRow + 1);
            case VALIDATION_ERROR_GENERAL:
                return "Validation failed: General validation error";
            default:
                return "Unknown validation error";
        }
    }

    public List<String> validateWithDetailedErrors(DataConverter converter) {
        validationErrors.clear();

        if (converter == null || converter.getData().isEmpty() || converter.getRows() <= 0
                || converter.getCols() <= 0) {
            validationErrors.add("General validation error: Data is missing or empty");
            return validationErrors;
        }

        int luftTempIdx = findColumnIndex(converter, "Luft_temperatur");
        int luftfugtighedIdx = findColumnIndex(converter, "Luftfugtighed");
        int jordFugtighedIdx = findColumnIndex(converter, "Jord_fugtighed");
        int lysTypeIdx = findColumnIndex(converter, "Lys_type");
        int lysHøjesteIdx = findColumnIndex(converter, "Lys_højeste_intensitet");
        int lysLavesteIdx = findColumnIndex(converter, "Lys_laveste_intensitet");
        int lysIndstillingIdx = findColumnIndex(converter, "Lys_indstilling");
        int lysGennemsnitIdx = findColumnIndex(converter, "Lys_gennemsnit");
        int afstandTilHøjdeIdx = findColumnIndex(converter, "Afstand_til_Højde");
        int vandTidFraSidsteIdx = findColumnIndex(converter, "Vand_tid_fra_sidste");
        int vandMængdeIdx = findColumnIndex(converter, "Vand_mængde");
        int vandFrekvensIdx = findColumnIndex(converter, "Vand_frekvens");
        int tidsstempelIdx = findColumnIndex(converter, "Tidsstempel");

        if (luftTempIdx == -1) {
            validationErrors.add("Required column 'Luft_temperatur' is missing");
        }

        if (luftfugtighedIdx == -1) {
            validationErrors.add("Required column 'Luftfugtighed' is missing");
        }

        if (jordFugtighedIdx == -1) {
            validationErrors.add("Required column 'Jord_fugtighed' is missing");
        }

        if (!validationErrors.isEmpty()) {
            return validationErrors;
        }

        for (int row = 0; row < converter.getRows(); row++) {
            Map<String, String> rowData = converter.getData().get(row);

            Float tempValue = parseFloat(rowData.get(converter.getHeaders().get(luftTempIdx)));
            if (tempValue == null) {
                validationErrors
                        .add(String.format("Row %d: Air temperature value is missing or not a valid number", row + 1));
            } else if (tempValue < 15.0f || tempValue > 40.0f) {
                validationErrors.add(String.format("Row %d: Air temperature must be between 15°C and 40°C (got: %s°C)",
                        row + 1, tempValue));
            }

            Integer luftfugtighedValue = parseInt(rowData.get(converter.getHeaders().get(luftfugtighedIdx)));
            if (luftfugtighedValue == null) {
                validationErrors
                        .add(String.format("Row %d: Air humidity value is missing or not a valid number", row + 1));
            } else if (luftfugtighedValue < 0 || luftfugtighedValue > 100) {
                validationErrors.add(String.format("Row %d: Air humidity must be between 0%% and 100%% (got: %s%%)",
                        row + 1, luftfugtighedValue));
            }

            Integer jordFugtighedValue = parseInt(rowData.get(converter.getHeaders().get(jordFugtighedIdx)));
            if (jordFugtighedValue == null) {
                validationErrors
                        .add(String.format("Row %d: Soil moisture value is missing or not a valid number", row + 1));
            } else if (jordFugtighedValue < 0 || jordFugtighedValue > 100) {
                validationErrors.add(String.format("Row %d: Soil moisture must be between 0%% and 100%% (got: %s%%)",
                        row + 1, jordFugtighedValue));
            }

            if (lysHøjesteIdx != -1) {
                Integer lysHøjesteValue = parseInt(rowData.get(converter.getHeaders().get(lysHøjesteIdx)));
                if (lysHøjesteValue == null) {
                    validationErrors.add(String
                            .format("Row %d: Highest light intensity value is missing or not a valid number", row + 1));
                } else if (lysHøjesteValue <= 0) {
                    validationErrors
                            .add(String.format("Row %d: Highest light intensity must be a positive number (got: %s)",
                                    row + 1, lysHøjesteValue));
                }

                if (lysLavesteIdx != -1) {
                    Integer lysLavesteValue = parseInt(rowData.get(converter.getHeaders().get(lysLavesteIdx)));
                    if (lysLavesteValue == null) {
                        validationErrors.add(String.format(
                                "Row %d: Lowest light intensity value is missing or not a valid number", row + 1));
                    } else if (lysLavesteValue < 0) {
                        validationErrors.add(
                                String.format("Row %d: Lowest light intensity must be a non-negative number (got: %s)",
                                        row + 1, lysLavesteValue));
                    } else if (lysHøjesteValue != null && lysHøjesteValue <= lysLavesteValue) {
                        validationErrors.add(String.format(
                                "Row %d: Highest light intensity (%s) must be greater than lowest light intensity (%s)",
                                row + 1, lysHøjesteValue, lysLavesteValue));
                    }
                }
            }

            if (lysIndstillingIdx != -1) {
                String lysIndstillingStr = rowData.get(converter.getHeaders().get(lysIndstillingIdx));
                Integer lysIndstillingValue = parseInt(lysIndstillingStr);
                if (lysIndstillingStr != null && !lysIndstillingStr.isEmpty() && lysIndstillingValue != null) {
                    // If it's a number, validate it
                    if (lysIndstillingValue < 0 || lysIndstillingValue > 10) {
                        validationErrors.add(String.format("Row %d: Light setting must be between 0 and 10 (got: %s)",
                                row + 1, lysIndstillingValue));
                    }
                }
            }

            if (afstandTilHøjdeIdx != -1) {
                Integer afstandValue = parseInt(rowData.get(converter.getHeaders().get(afstandTilHøjdeIdx)));
                if (afstandValue == null) {
                    validationErrors.add(
                            String.format("Row %d: Height distance value is missing or not a valid number", row + 1));
                } else if (afstandValue <= 0) {
                    validationErrors.add(String.format("Row %d: Height distance must be a positive number (got: %s)",
                            row + 1, afstandValue));
                }
            }

            if (vandTidFraSidsteIdx != -1) {
                Integer vandTidValue = parseInt(rowData.get(converter.getHeaders().get(vandTidFraSidsteIdx)));
                if (vandTidValue == null) {
                    validationErrors.add(String.format(
                            "Row %d: Time since last watering value is missing or not a valid number", row + 1));
                } else if (vandTidValue < 0) {
                    validationErrors.add(
                            String.format("Row %d: Time since last watering must be a non-negative number (got: %s)",
                                    row + 1, vandTidValue));
                }
            }

            if (vandMængdeIdx != -1) {
                Integer vandMængdeValue = parseInt(rowData.get(converter.getHeaders().get(vandMængdeIdx)));
                if (vandMængdeValue == null) {
                    validationErrors
                            .add(String.format("Row %d: Water amount value is missing or not a valid number", row + 1));
                } else if (vandMængdeValue <= 0) {
                    validationErrors.add(String.format("Row %d: Water amount must be a positive number (got: %s)",
                            row + 1, vandMængdeValue));
                }
            }

            if (vandFrekvensIdx != -1) {
                Integer vandFrekvensValue = parseInt(rowData.get(converter.getHeaders().get(vandFrekvensIdx)));
                if (vandFrekvensValue == null) {
                    validationErrors.add(
                            String.format("Row %d: Water frequency value is missing or not a valid number", row + 1));
                } else if (vandFrekvensValue <= 0) {
                    validationErrors.add(String.format("Row %d: Water frequency must be a positive number (got: %s)",
                            row + 1, vandFrekvensValue));
                }
            }

            if (tidsstempelIdx != -1) {
                String timestampStr = rowData.get(converter.getHeaders().get(tidsstempelIdx));
                if (!isValidTimestamp(timestampStr)) {
                    validationErrors
                            .add(String.format("Row %d: Timestamp must be in YYYY-MM-DDThh:mm:ss format (got: %s)",
                                    row + 1, timestampStr));
                }
            }
        }

        return validationErrors;
    }

    public void validateWithException(DataConverter converter) throws ValidationException {
        List<String> errors = validateWithDetailedErrors(converter);

        if (!errors.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("Validation failed with the following errors:\n");
            for (String error : errors) {
                errorMsg.append("- ").append(error).append("\n");
            }

            logger.error("Validation failed: {}", errorMsg);
            throw new ValidationException(errorMsg.toString());
        }

        logger.info("Validation successful");
    }

    private int findColumnIndex(DataConverter converter, String columnName) {
        for (int i = 0; i < converter.getHeaders().size(); i++) {
            if (converter.getHeaders().get(i).equals(columnName)) {
                return i;
            }
        }
        return -1;
    }

    private Float parseFloat(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isValidTimestamp(String value) {
        if (value == null || value.length() != 19) {
            return false;
        }

        if (value.charAt(4) != '-' || value.charAt(7) != '-' ||
                value.charAt(10) != 'T' || value.charAt(13) != ':' ||
                value.charAt(16) != ':') {
            return false;
        }

        try {
            LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
