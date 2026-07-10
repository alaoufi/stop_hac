package com.privacyshield.monitor.data.db

import androidx.room.TypeConverter
import com.privacyshield.monitor.core.model.RiskLevel
import com.privacyshield.monitor.core.model.SensorType
import com.privacyshield.monitor.core.model.UserDecision
import com.privacyshield.monitor.core.model.WhitelistScope

/** Stores enums as their stable string names so the schema survives reordering. */
class Converters {
    @TypeConverter fun sensorToString(v: SensorType): String = v.name
    @TypeConverter fun stringToSensor(v: String): SensorType = SensorType.valueOf(v)

    @TypeConverter fun riskToString(v: RiskLevel): String = v.name
    @TypeConverter fun stringToRisk(v: String): RiskLevel = RiskLevel.valueOf(v)

    @TypeConverter fun decisionToString(v: UserDecision): String = v.name
    @TypeConverter fun stringToDecision(v: String): UserDecision = UserDecision.valueOf(v)

    @TypeConverter fun scopeToString(v: WhitelistScope): String = v.name
    @TypeConverter fun stringToScope(v: String): WhitelistScope = WhitelistScope.valueOf(v)
}
