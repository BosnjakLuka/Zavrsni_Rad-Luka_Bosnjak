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
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FxmlControllerContractTest {

    @Test
    void fxmlResourcesReferenceExistingControllerFieldsAndActions() throws Exception {
        List<FormContract> forms = List.of(
                new FormContract("/hr/lukabosnjak/ui/view/login.fxml", LoginController.class),
                new FormContract("/hr/lukabosnjak/ui/view/registration.fxml", RegistrationController.class),
                new FormContract("/hr/lukabosnjak/ui/view/main-form.fxml", MainFormController.class),
                new FormContract("/hr/lukabosnjak/ui/view/material-type-form.fxml", MaterialTypeFormController.class),
                new FormContract("/hr/lukabosnjak/ui/view/cnc-machine-form.fxml", CncMachineFormController.class),
                new FormContract("/hr/lukabosnjak/ui/view/tool-form.fxml", ToolFormController.class));

        for (FormContract form : forms) {
            verify(form);
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
