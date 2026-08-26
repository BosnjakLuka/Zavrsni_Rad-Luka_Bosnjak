package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.MachiningJob;
import hr.lukabosnjak.gcode.GCodeGenerator;
import hr.lukabosnjak.gcode.GCodeProgram;
import hr.lukabosnjak.geometry.ToolPath;
import hr.lukabosnjak.geometry.ToolPathCompensationService;
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
    private final ToolPathCompensationService toolPathCompensationService;
    private final GCodeGenerator gCodeGenerator;

    public ProgramGenerationService(
            MachiningJobValidator machiningJobValidator,
            ToolPathService toolPathService,
            SingleShapeFitValidator singleShapeFitValidator,
            ToolPathCompensationService toolPathCompensationService,
            GCodeGenerator gCodeGenerator
    ) {
        this.machiningJobValidator = Objects.requireNonNull(machiningJobValidator);
        this.toolPathService = Objects.requireNonNull(toolPathService);
        this.singleShapeFitValidator = Objects.requireNonNull(singleShapeFitValidator);
        this.toolPathCompensationService = Objects.requireNonNull(toolPathCompensationService);
        this.gCodeGenerator = Objects.requireNonNull(gCodeGenerator);
    }

    public GCodeProgram generate(ProgramGenerationRequest request) {
        Objects.requireNonNull(request, "request");

        MachiningJob job = new MachiningJob(
                null, null, request.machine(), request.tool(), request.materialSheet(),
                request.machiningParameters(), request.shape(), "Preview", SINGLE_ELEMENT_QUANTITY,
                null, null, null);
        machiningJobValidator.validate(job);

        ToolPath programmedContour = toolPathService.generate(request.shape());
        ToolPath cutterCenterPath = toolPathCompensationService.compensate(
                programmedContour, request.tool(), request.cutSide());
        singleShapeFitValidator.validate(cutterCenterPath, request.materialSheet(), request.machine());
        return gCodeGenerator.generate(cutterCenterPath, request.machiningParameters());
    }
}
