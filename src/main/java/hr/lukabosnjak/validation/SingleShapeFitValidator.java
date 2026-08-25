package hr.lukabosnjak.validation;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.geometry.Bounds2;
import hr.lukabosnjak.geometry.ToolPath;
import hr.lukabosnjak.geometry.ToolPathBoundsCalculator;

import java.util.Objects;

public final class SingleShapeFitValidator {
    private final ToolPathBoundsCalculator boundsCalculator;
    private final MaterialSheetValidator materialSheetValidator;

    public SingleShapeFitValidator(
            ToolPathBoundsCalculator boundsCalculator,
            MaterialSheetValidator materialSheetValidator
    ) {
        this.boundsCalculator = Objects.requireNonNull(boundsCalculator);
        this.materialSheetValidator = Objects.requireNonNull(materialSheetValidator);
    }

    public void validate(ToolPath toolPath, MaterialSheet materialSheet, CncMachine machine) {
        if (toolPath == null) {
            throw new ValidationException("Putanja alata mora biti zadana.");
        }
        materialSheetValidator.validate(materialSheet);
        if (machine == null) {
            throw new ValidationException("CNC stroj mora biti odabran.");
        }

        Bounds2 bounds = boundsCalculator.calculate(toolPath);
        if (!fitsWithin(bounds, materialSheet.getWidth(), materialSheet.getHeight())) {
            throw new ValidationException("Oblik ne stane na odabranu ploču materijala.");
        }
        if (!fitsWithin(bounds, machine.getWorkAreaX(), machine.getWorkAreaY())) {
            throw new ValidationException(
                    "Planirani XY opseg ne stane u radno područje odabranog CNC stroja.");
        }
    }

    private boolean fitsWithin(Bounds2 bounds, double limitX, double limitY) {
        return bounds.minX() >= 0
                && bounds.minY() >= 0
                && bounds.maxX() <= limitX
                && bounds.maxY() <= limitY;
    }
}
