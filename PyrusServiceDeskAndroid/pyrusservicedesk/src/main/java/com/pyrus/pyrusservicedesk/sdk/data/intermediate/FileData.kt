package com.pyrus.pyrusservicedesk.sdk.data.intermediate

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

/**
 * Intermediate data for transfer attachment data between UI elements.
 * @param uri can contain either url of the server attachment or uri of the local file.
 */
internal data class FileData(val fileName: String,
                             val bytesSize: Int,
                             val uri: Uri,
                             val isLocal: Boolean) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readInt(),
        parcel.readParcelable(Uri::class.java.classLoader),
        parcel.readByte() != 0.toByte()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(fileName)
        parcel.writeInt(bytesSize)
        parcel.writeParcelable(uri, flags)
        parcel.writeByte(if (isLocal) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FileData> {
        override fun createFromParcel(parcel: Parcel): FileData {
            return FileData(parcel)
        }

        override fun newArray(size: Int): Array<FileData?> {
            return arrayOfNulls(size)
        }
    }
}