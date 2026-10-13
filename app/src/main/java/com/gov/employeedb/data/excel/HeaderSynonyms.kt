package com.gov.employeedb.data.excel

import com.gov.employeedb.domain.model.EmployeeField

/**
 * The trilingual header vocabulary from PLAN.md §6.2, raw as authored. [ColumnDetector] normalizes
 * both sides through [HeaderNormalizer] before scoring, so accented/`ال`-prefixed variants collapse
 * onto the same key without duplicating entries here (e.g. one entry covers `é`/`e`, `الأجر`/`الاجر`).
 *
 * The lookup keys are the **normalized** forms; this object is a spec table, not the scorer.
 */
object HeaderSynonyms {

    val TERMS: Map<EmployeeField, List<String>> = mapOf(
        EmployeeField.FULL_NAME to listOf(
            "name", "full name", "employee name", "employee",
            "nom", "nom complet", "nom et prenom", "employe",
            "الاسم", "الاسم الكامل", "اسم الموظف", "الموظف",
        ),
        EmployeeField.DATE_OF_BIRTH to listOf(
            "dob", "date of birth", "birth date", "born",
            "date de naissance", "naissance", "date naissance",
            "تاريخ الميلاد", "الميلاد", "تاريخ الولادة",
        ),
        EmployeeField.HIRE_DATE to listOf(
            "hire date", "hiring date", "date of hire", "start date", "recruitment date",
            "date d'embauche", "embauche", "recrutement", "date de recrutement",
            "تاريخ التعيين", "التعيين", "تاريخ التوظيف", "التوظيف",
        ),
        EmployeeField.DEPARTMENT to listOf(
            "department", "dept", "division", "directorate",
            "departement", "service", "direction", "division",
            "الإدارة", "القسم", "الدائرة", "المديرية", "المصلحة",
        ),
        EmployeeField.POSITION to listOf(
            "position", "job title", "title", "post", "function",
            "poste", "fonction", "emploi", "intitule",
            "الوظيفة", "المنصب", "المسمى الوظيفي",
        ),
        EmployeeField.GRADE to listOf(
            "grade", "class", "classification", "scale", "rank",
            "grade", "echelle", "categorie", "classe",
            "الدرجة", "الرتبة", "الصنف", "الفئة",
        ),
        EmployeeField.SALARY to listOf(
            "salary", "wage", "pay", "remuneration", "gross salary",
            "salaire", "traitement", "remuneration", "paie",
            "الأجر", "الراتب", "المعاش", "الأجر الإجمالي",
        ),
        EmployeeField.EMAIL to listOf(
            "email", "e-mail", "mail", "courriel",
            "email", "courriel", "mail",
            "البريد الإلكتروني", "الإيميل", "البريد",
        ),
        EmployeeField.PHONE to listOf(
            "phone", "mobile", "telephone", "tel", "cell",
            "telephone", "tel", "portable", "mobile",
            "الهاتف", "الجوال", "رقم الهاتف", "الهاتف المحمول",
        ),
        EmployeeField.ADDRESS to listOf(
            "address", "residence", "domicile",
            "adresse", "residence", "domicile",
            "العنوان", "السكن", "محل الإقامة",
        ),
        EmployeeField.NOTES to listOf(
            "notes", "note", "remarks", "comments", "observations",
            "notes", "remarque", "remarques", "observations",
            "ملاحظات", "تعليق", "تعليقات", "ملحوظة",
        ),
    )

    /** Pre-normalized keys for scoring; computed once at class load. */
    val normalizedKeys: Map<EmployeeField, Set<String>> = TERMS.mapValues { (_, terms) ->
        terms.mapTo(mutableSetOf()) { HeaderNormalizer.normalize(it) }
    }

    /** Every normalized synonym, flattened by field, for header-row discovery scoring. */
    val allNormalized: Map<String, EmployeeField> by lazy {
        buildMap {
            normalizedKeys.forEach { (field, keys) -> keys.forEach { key -> put(key, field) } }
        }
    }
}