package sonar.reactorbuilder.common.reactors;

import sonar.reactorbuilder.common.reactors.templates.AbstractTemplate;
import sonar.reactorbuilder.common.reactors.templates.OverhaulFissionTemplate;
import sonar.reactorbuilder.common.reactors.templates.OverhaulTurbine;
import sonar.reactorbuilder.common.reactors.templates.UnderhaulSFRTemplate;
import sonar.reactorbuilder.common.reactors.templates.casingaware.overhaul.CasingAwareOverhaulFissionSFR;
import sonar.reactorbuilder.common.reactors.templates.casingaware.overhaul.CasingAwareOverhaulTurbine;

public enum TemplateType {
    UNDERHAUL_SFR(false, "Underhaul SFR", UnderhaulSFRTemplate::new),
    OVERHAUL_SFR(true, "Overhaul SFR", OverhaulFissionTemplate.SFR::new),
    OVERHAUL_MSR(true, "Overhaul MSR", OverhaulFissionTemplate.MSR::new),

    @Deprecated
    OVERHAUL_TURBINE(true, "Overhaul Turbine", OverhaulTurbine::new),

    CASINGAWARE_OVERHAUL_SFR(true, "Overhaul SFR", CasingAwareOverhaulFissionSFR::new),
    CASINGAWARE_OVERHAUL_TURBINE(true, "Overhaul Turbine", CasingAwareOverhaulTurbine::new);

    public final boolean overhaul;
    public final String fileType;
    public final IReactorProvider creator;

    TemplateType(boolean overhaul, String s, IReactorProvider creator) {
        this.overhaul = overhaul;
        this.fileType = s;
        this.creator = creator;
    }

    public interface IReactorProvider {

        AbstractTemplate create();

    }
}
