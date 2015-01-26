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
package io.dwak.reactor.intellij.plugin;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import groovy.ui.SystemOutputInterceptor;

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

    private String checkFieldPrefixes(String fieldName){
        String newFieldName = fieldName;
        if(fieldName.charAt(0) == 'm'
                && Character.isLowerCase(fieldName.charAt(0))
                && Character.isUpperCase(fieldName.charAt(1))){
            newFieldName = fieldName.substring(1, fieldName.length());
        }

        return newFieldName;
    }

    private String generateGetters(PsiField field, PsiClass psiClass){
        String fieldName = checkFieldPrefixes(field.getName());
        String methodPrefix = field.getType().equals(PsiType.BOOLEAN)
                ? "is"
                : "get";

        boolean isBooleanWithPrefix = false;
        if(fieldName.toLowerCase().startsWith("is")){
            isBooleanWithPrefix = true;
        }

        String fieldNameForMethodName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1, fieldName.length());
        if(isBooleanWithPrefix){
            fieldNameForMethodName = fieldNameForMethodName.substring(2, fieldNameForMethodName.length());
        }
        String type = field.getType().getPresentableText();
        type = type.replace("ReactorVar<", "");
        type = type.replace(">", "");
        StringBuilder sb = new StringBuilder(
                "public " + type
                        + " "
                        + methodPrefix + fieldNameForMethodName + "() {");
        sb.append("return " + field.getName()+ ".getValue();");
        sb.append("}");

        return sb.toString();
    }

    private String generateSetters(PsiField field, PsiClass psiClass){
        String fieldName = checkFieldPrefixes(field.getName());
        final String setterParameter = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1, fieldName.length());
        String type = field.getType().getPresentableText();
        type = type.replace("ReactorVar<", "");
        type = type.replace(">", "");
        StringBuilder sb = new StringBuilder(
                "public void"
                        + " set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1, fieldName.length())
                        + "(" + type + " " + setterParameter + ") {");
        sb.append("this."+ field.getName() + ".setValue(" + setterParameter + ");");
        sb.append("}");

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
