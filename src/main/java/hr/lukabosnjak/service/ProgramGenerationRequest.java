package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.CncMachine;
import hr.lukabosnjak.domain.entities.MachiningParameters;
import hr.lukabosnjak.domain.entities.MaterialSheet;
import hr.lukabosnjak.domain.entities.Shape;
import hr.lukabosnjak.domain.entities.Tool;

/** Input for generating one CNC program; quantity is intentionally not an input in iteration 1. */
public record ProgramGenerationRequest(
        CncMachine machine,
        Tool tool,
        MaterialSheet materialSheet,
        MachiningParameters machiningParameters,
        Shape shape
) {
}
