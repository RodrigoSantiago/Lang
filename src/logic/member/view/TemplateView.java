package logic.member.view;

import content.Token;
import logic.Pointer;
import logic.templates.Generic;
import logic.templates.Template;

public class TemplateView {

    public final Template template;
    private Pointer[] typePtrs;

    public TemplateView(Pointer caller, Template template) {
        this.template = template;

        typePtrs = new Pointer[template.getCount()];
        for (int i = 0; i < template.getCount(); i++) {
            if (Pointer.hasGeneric(template.getBasePtr(i), caller)) {
                typePtrs[i] = Pointer.byGeneric(template.getBasePtr(i), caller);
            }
        }
    }

    public TemplateView(Pointer caller, TemplateView templateView) {
        this.template = templateView.template;

        typePtrs = new Pointer[templateView.getGenCount()];
        for (int i = 0; i < templateView.getGenCount(); i++) {
            if (Pointer.hasGeneric(templateView.getGenTypePtr(i), caller)) {
                typePtrs[i] = Pointer.byGeneric(templateView.getGenTypePtr(i), caller);
            }
        }
    }

    public int getGenCount() {
        return template.getCount();
    }

    public Pointer getGenTypePtr(int index) {
        if (typePtrs == null || typePtrs[index] == null) {
            return template.getBasePtr(index);
        }
        return typePtrs[index];
    }

    public Token getGenName(int index) {
        return template.getNameToken(index);
    }

    public Generic getGeneric(int i) {
        return template.getGeneric(i);
    }
}
