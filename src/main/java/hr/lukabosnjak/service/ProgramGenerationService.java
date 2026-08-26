package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.gcode.GCodeGenerator;
import hr.lukabosnjak.gcode.GCodeProgram;
import hr.lukabosnjak.geometry.ToolPath;
import hr.lukabosnjak.geometry.ToolPathService;
import hr.lukabosnjak.validation.MachiningJobValidator;
import hr.lukabosnjak.validation.SingleShapeFitValidator;

import java.util.Objects;

/** Coordinates the single-element generation workflow without persistence or layout behavior. */
public final class ProgramGenerationService {
    private static final int SINGLE_ELEMENT_QUANTITY = 1;

    private final MachiningJobValidator machiningJobValidator;
    private final ToolPathService toolPathService;
    private final SingleShapeFitValidator singleShapeFitValidator;
    private final GCodeGenerator gCodeGenerator;

    public ProgramGenerationService(
            MachiningJobValidator machiningJobValidator,
            ToolPathService toolPathService,
            SingleShapeFitValidator singleShapeFitValidator,
            GCodeGenerator gCodeGenerator
    ) {
        this.machiningJobValidator = Objects.requireNonNull(machiningJobValidator);
        this.toolPathService = Objects.requireNonNull(toolPathService);
        this.singleShapeFitValidator = Objects.requireNonNull(singleShapeFitValidator);
        this.gCodeGenerator = Objects.requireNonNull(gCodeGenerator);
    }

    public GCodeProgram generate(ProgramGenerationRequest request) {
        Objects.requireNonNull(request, "request");

        MachiningJob job = new MachiningJob(
                null, null, request.machine(), request.tool(), request.materialSheet(),
                request.machiningParameters(), request.shape(), "Preview", SINGLE_ELEMENT_QUANTITY,
                null, null, null);
        machiningJobValidator.validate(job);

        ToolPath toolPath = toolPathService.generate(request.shape());
        singleShapeFitValidator.validate(toolPath, request.materialSheet(), request.machine());
        return gCodeGenerator.generate(toolPath, request.machiningParameters());
    }
}
