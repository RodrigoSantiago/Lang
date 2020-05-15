package logic.member.view;

import content.Token;
import logic.templates.Generic;
import logic.Pointer;
import logic.templates.Template;

public class TemplateView {

    private Template template;
    private Pointer[] typePtrs;

    public TemplateView(Pointer caller, Template template) {
        this.template = template;

        typePtrs = new Pointer[template.generics.size()];
        for (int i = 0; i < template.generics.size(); i++) {
            Generic gen = template.generics.get(i);
            if (Pointer.hasGeneric(gen.basePtr, caller)) {
                typePtrs[i] = Pointer.byGeneric(gen.basePtr, caller);
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
        return template.generics.size();
    }

    public Pointer getGenTypePtr(int index) {
        if (typePtrs == null || typePtrs[index] == null) {
            return template.generics.get(index).basePtr;
        }
        return typePtrs[index];
    }

    public Token getGenName(int index) {
        return template.generics.get(index).nameToken;
    }
}
