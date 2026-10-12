package rahul.jagtap.dmas.model

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GovtSchemeInfo(
    @SerializedName("email") @Expose var email: String? = "",
    @SerializedName("createdBy") @Expose var createdBy: String? = "",
    @SerializedName("createdByUserName") @Expose var createdByUserName: String? = "",
    @SerializedName("uid") @Expose var uid: String? = "",
    @SerializedName("createdDateTime") @Expose var createdDateTime: String? = "",
    @SerializedName("title") @Expose var title: String? = "",
    @SerializedName("titleColor") @Expose var titleColor: String? = "", // orange, yellow, blue, pink, golden, light green
    @SerializedName("schemeInfo") @Expose var schemeInfo: String? = "",
    @SerializedName("pushKey") @Expose var pushKey: String? = "",
    @SerializedName("timeStamp") @Expose var timeStamp: Long? = 0
) : Parcelable {
    constructor() : this("", "", "", "", "","", "", "", "", -1)
}