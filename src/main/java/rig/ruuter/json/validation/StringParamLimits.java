package rig.ruuter.json.validation;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
public class StringParamLimits {
    @NotBlank
    private String input;
    @Min(value = 1)
    private Integer min;
    @Min(value = 1)
    private Integer max;
}
