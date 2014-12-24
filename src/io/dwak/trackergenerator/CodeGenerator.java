/*
 * Copyright (C) 2013 Michał Charmas (http://blog.charmas.pl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.dwak.trackergenerator;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.VariableKind;

import java.util.ArrayList;
import java.util.List;


/**
 * Quite a few changes here by Dallas Gutauckis [dallas@gutauckis.com]
 */
public class CodeGenerator {
    public static final String CREATOR_NAME = "CREATOR";

    private final PsiClass mClass;
    private final List<PsiField> mFields;

    public CodeGenerator(PsiClass psiClass, List<PsiField> fields) {
        mClass = psiClass;
        mFields = fields;

    }

    private String generateGetters(PsiField field, PsiClass psiClass){
        String fieldName = field.getName();
        StringBuilder sb = new StringBuilder(
                "public "+ field.getType().getCanonicalText()
                        + " get"+ fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1, fieldName.length())+ "() {");
        sb.append(fieldName + "Dep.depend();");
        sb.append("return " + fieldName + ";");
        sb.append("}");

        return sb.toString();
    }

    private String generateSetters(PsiField field, PsiClass psiClass){
        String fieldName = field.getName();
        StringBuilder sb = new StringBuilder(
                "public " + field.getType().getCanonicalText()
                        + " set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1, fieldName.length())
                        + "(" + field.getType().getCanonicalText() + " " + fieldName + ") {");
        sb.append("this."+ fieldName + " = " + fieldName + ";");
        sb.append(fieldName + "Dep.changed();");
        sb.append("}");

        return sb.toString();
    }

    private String generateTrackerDependencyFields(PsiField field, PsiClass psiClass){
        String fieldName = field.getName();
        StringBuilder sb = new StringBuilder("private TrackerDependency " + fieldName + "Dep = new TrackerDependency();");
        return sb.toString();
    }

    public void generate() {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(mClass.getProject());

        // Getters
        List<PsiMethod> psiMethods = new ArrayList<PsiMethod>();
        List<PsiField> psiFields = new ArrayList<PsiField>();
        for (PsiField mField : mFields) {
            psiMethods.add(elementFactory.createMethodFromText(generateGetters(mField, mClass), mClass));
            psiMethods.add(elementFactory.createMethodFromText(generateSetters(mField, mClass), mClass));
            psiFields.add(elementFactory.createFieldFromText(generateTrackerDependencyFields(mField, mClass), mClass));
        }

        JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(mClass.getProject());
        for (PsiMethod psiMethod : psiMethods) {
            styleManager.shortenClassReferences(mClass.addBefore(psiMethod, mClass.getLastChild()));
        }

        for (PsiField psiField : psiFields) {
            styleManager.shortenClassReferences(mClass.add(psiField));
        }
    }
}
