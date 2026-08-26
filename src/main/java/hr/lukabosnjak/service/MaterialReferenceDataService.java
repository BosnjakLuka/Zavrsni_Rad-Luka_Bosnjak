package hr.lukabosnjak.service;

import hr.lukabosnjak.domain.entities.MaterialType;
import hr.lukabosnjak.persistence.repository.MaterialTypeRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/** Reads persisted material types for UI selection. */
public final class MaterialReferenceDataService {
    private final MaterialTypeRepository materialTypeRepository;

    public MaterialReferenceDataService(MaterialTypeRepository materialTypeRepository) {
        this.materialTypeRepository = Objects.requireNonNull(materialTypeRepository);
    }

    public List<MaterialType> loadMaterialTypes() {
        try {
            return materialTypeRepository.findAll();
        } catch (SQLException exception) {
            throw new ReferenceDataAccessException("Učitavanje vrsta materijala nije uspjelo.", exception);
        }
    }
}
