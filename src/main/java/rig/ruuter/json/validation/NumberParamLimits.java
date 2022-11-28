package rig.ruuter.json.validation;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class NumberParamLimits {
    @NotNull
    private Double input;
    private Double min;
    private Double max;
}
