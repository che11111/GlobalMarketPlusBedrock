package dev.che1s.globalmarketplusbedrock;

import java.util.Arrays;
import java.util.function.Consumer;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.response.CustomFormResponse;

/** Custom forms cannot contain buttons: append an explicit Submit/Back action selector. */
final class CustomFormNavigation {
    private CustomFormNavigation() {}

    static Consumer<CustomFormResponse> attach(CustomForm.Builder form, String label, String submit,
            String backLabel, Consumer<CustomFormResponse> action, Runnable back) {
        int componentIndex = form.build().content().size();
        form.dropdown(label, Arrays.asList(submit, backLabel), 0);
        return response -> {
            if (response.asDropdown(componentIndex) == 1) {
                back.run();
            } else {
                response.reset();
                response.includeLabels(false);
                action.accept(response);
            }
        };
    }
}
