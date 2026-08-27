package hr.lukabosnjak.ui.controller;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxmlControllerContractTest {

    @Test
    void fxmlResourcesReferenceExistingControllerFieldsAndActions() throws Exception {
        List<FormContract> forms = List.of(
                new FormContract("/hr/lukabosnjak/ui/view/login.fxml", LoginController.class),
                new FormContract("/hr/lukabosnjak/ui/view/registration.fxml", RegistrationController.class),
                new FormContract("/hr/lukabosnjak/ui/view/main-form.fxml", MainFormController.class),
                new FormContract("/hr/lukabosnjak/ui/view/material-type-form.fxml", MaterialTypeFormController.class),
                new FormContract("/hr/lukabosnjak/ui/view/cnc-machine-form.fxml", CncMachineFormController.class),
                new FormContract("/hr/lukabosnjak/ui/view/tool-form.fxml", ToolFormController.class),
                new FormContract("/hr/lukabosnjak/ui/view/user-management.fxml", UserManagementController.class),
                new FormContract("/hr/lukabosnjak/ui/view/saved-programs.fxml", SavedProgramsController.class),
                new FormContract("/hr/lukabosnjak/ui/view/catalog.fxml", CatalogController.class));

        for (FormContract form : forms) {
            verify(form);
        }
    }

    @Test
    void catalogProvidesMachineSelectorAboveToolList() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(
                "/hr/lukabosnjak/ui/view/catalog.fxml")) {
            assertNotNull(stream);
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            NodeList comboBoxes = document.getElementsByTagName("ComboBox");
            boolean selectorFound = false;
            for (int index = 0; index < comboBoxes.getLength(); index++) {
                Element comboBox = (Element) comboBoxes.item(index);
                if ("toolMachineComboBox".equals(comboBox.getAttribute("fx:id"))) {
                    assertEquals("#handleToolMachineChanged", comboBox.getAttribute("onAction"));
                    selectorFound = true;
                }
            }
            assertTrue(selectorFound, "catalog must provide a machine selector for the tool list");
        }
    }

    @Test
    void catalogPlacesSavedProgramsBelowReferenceDataCards() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(
                "/hr/lukabosnjak/ui/view/catalog.fxml")) {
            assertNotNull(stream);
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            NodeList elements = document.getElementsByTagName("*");
            int referenceDataCardsIndex = -1;
            int savedProgramsIndex = -1;
            Element savedPrograms = null;
            for (int index = 0; index < elements.getLength(); index++) {
                Element element = (Element) elements.item(index);
                if ("FlowPane".equals(element.getTagName())) {
                    referenceDataCardsIndex = index;
                }
                if ("savedProgramsList".equals(element.getAttribute("fx:id"))) {
                    savedProgramsIndex = index;
                    savedPrograms = element;
                }
            }
            assertTrue(referenceDataCardsIndex >= 0);
            assertTrue(savedProgramsIndex > referenceDataCardsIndex,
                    "saved programs must be declared below the three reference-data cards");
            for (var parent = savedPrograms.getParentNode(); parent != null; parent = parent.getParentNode()) {
                assertFalse("FlowPane".equals(parent.getNodeName()),
                        "saved programs must not be placed beside the reference-data cards");
            }
        }
    }

    private void verify(FormContract form) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(form.resourcePath())) {
            assertNotNull(stream, form.resourcePath());
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            assertEquals(form.controllerType().getName(),
                    document.getDocumentElement().getAttribute("fx:controller"));

            NodeList elements = document.getElementsByTagName("*");
            for (int index = 0; index < elements.getLength(); index++) {
                Element element = (Element) elements.item(index);
                String fieldName = element.getAttribute("fx:id");
                if (!fieldName.isBlank()) {
                    assertDoesNotThrow(() -> form.controllerType().getDeclaredField(fieldName),
                            form.resourcePath() + " fx:id=" + fieldName);
                }
                String action = element.getAttribute("onAction");
                if (action.startsWith("#")) {
                    String methodName = action.substring(1);
                    assertDoesNotThrow(() -> form.controllerType().getDeclaredMethod(methodName),
                            form.resourcePath() + " onAction=" + action);
                }
            }
        }
    }

    private record FormContract(String resourcePath, Class<?> controllerType) {
    }
}
