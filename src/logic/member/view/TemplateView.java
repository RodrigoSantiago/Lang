package logic.member.view;

import content.Token;
import logic.templates.Generic;
import logic.Pointer;
import logic.templates.Template;

public class TemplateView {

    Template template;
    Pointer caller;
    Pointer[] types;

    public TemplateView(Template template, Pointer caller) {
        this.template = template;
        this.caller = caller;

        types = new Pointer[template.generics.size()];
        for (int i = 0; i < template.generics.size(); i++) {
            Generic gen = template.generics.get(i);
            if (Pointer.hasGeneric(gen.type, caller)) {
                types[i] = Pointer.byGeneric(gen.type, caller);
            }
        }
    }

    public TemplateView(TemplateView templateView, Pointer caller) {
        this.template = templateView.template;
        this.caller = caller;

        types = new Pointer[templateView.getGenCount()];
        for (int i = 0; i < templateView.getGenCount(); i++) {
            if (Pointer.hasGeneric(templateView.getGenType(i), caller)) {
                types[i] = Pointer.byGeneric(templateView.getGenType(i), caller);
            }
        }
    }

    public int getGenCount() {
        return types.length;
    }

    public Pointer getGenType(int index) {
        Pointer type = types[index];
        if (type == null) {
            type = template.generics.get(index).type;
        }
        return type;
    }

    public Token getGenName(int index) {
        return template.generics.get(index).nameToken;
    }
}
