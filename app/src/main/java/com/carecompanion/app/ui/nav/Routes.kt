package com.carecompanion.app.ui.nav

/** Central route table. Guardian sub-screens carry the elder id as a path arg. */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val ROLE = "role"

    const val GUARDIAN_HOME = "guardian"
    const val ADD_ELDER = "guardian/addElder"

    // elder-scoped guardian screens
    const val ELDER_PROFILE = "guardian/elder/{elderId}/profile"
    const val CONTACTS = "guardian/elder/{elderId}/contacts"
    const val ADD_CONTACT = "guardian/elder/{elderId}/contacts/add"
    const val MEDICINES = "guardian/elder/{elderId}/medicines"
    const val ADD_MEDICINE = "guardian/elder/{elderId}/medicines/add"
    const val EDIT_MEDICINE = "guardian/elder/{elderId}/medicines/edit/{medicineId}"
    const val EDIT_CONTACT = "guardian/elder/{elderId}/contacts/edit/{contactId}"
    const val SETTINGS = "guardian/settings"
    const val SCHEDULE = "guardian/elder/{elderId}/schedule"
    const val SCHEDULE_BUILDER = "guardian/elder/{elderId}/schedule/build/{medicineId}"
    const val REMINDERS = "guardian/elder/{elderId}/reminders"
    const val VITALS = "guardian/elder/{elderId}/vitals"
    const val ADD_VITAL = "guardian/elder/{elderId}/vitals/add"
    const val ADHERENCE = "guardian/elder/{elderId}/adherence"
    const val SOS = "guardian/elder/{elderId}/sos"
    const val SOS_SETTINGS = "guardian/elder/{elderId}/sos/settings"
    const val FAMILY = "guardian/elder/{elderId}/family"
    const val OTT = "guardian/elder/{elderId}/ott"
    const val WHEELCHAIR = "guardian/elder/{elderId}/wheelchair"

    const val ELDER_APP = "elderapp"

    fun elder(route: String, elderId: String) = route.replace("{elderId}", elderId)
    fun scheduleBuilder(elderId: String, medicineId: String) =
        SCHEDULE_BUILDER.replace("{elderId}", elderId).replace("{medicineId}", medicineId)
    fun editMedicine(elderId: String, medicineId: String) =
        EDIT_MEDICINE.replace("{elderId}", elderId).replace("{medicineId}", medicineId)
    fun editContact(elderId: String, contactId: String) =
        EDIT_CONTACT.replace("{elderId}", elderId).replace("{contactId}", contactId)
}
