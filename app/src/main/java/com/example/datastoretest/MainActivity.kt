package com.example.datastoretest

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.application.User
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val saveButton=findViewById<Button>(R.id.tv_add)
        val getButton=findViewById<Button>(R.id.tv_get)
        val tvShow=findViewById<TextView>(R.id.tv_show)
        val tvProtoGet=findViewById<Button>(R.id.tv_proto_get)
        val tvProtoSave=findViewById<Button>(R.id.tv_proto_add)
        tvProtoSave.setOnClickListener {
           GlobalScope.launch {
               saveDataUseProto(2,"niko")
           }
        }
        tvProtoGet.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                getDataUseProto().collect { tvShow.setText(it.userName+" age:"+it.age) }
            }

        }
        saveButton.setOnClickListener {
            GlobalScope.launch {
                saveDataToDataStore("key1","value1")
            }
        }
        getButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                getDataFromDataStore("key1").collect { tvShow.setText(it) }
            }
        }
    }
    //导入旧项目方式
    val preDataStore2:DataStore<Preferences> by preferencesDataStore(name="user",produceMigrations ={listOf<DataMigration<Preferences>>(SharedPreferencesMigration(this,"user"))})
    //----preferences 方式
    val preDataStore:DataStore<Preferences> by preferencesDataStore(name="user")
    //保持数据到下的key键下方
    suspend fun saveDataToDataStore(key:String,value:String){
        preDataStore.edit { it[stringPreferencesKey(key)]=value }
    }
    //从key键取值
     fun getDataFromDataStore(key: String): Flow<String?> {
        return preDataStore.data.map { it[stringPreferencesKey(key)] }
    }
    //------proto 方式
   suspend fun saveDataUseProto(age : Int,name:String){
        userDataStore.updateData { user->
            user.toBuilder().setAge(age)
                .setUserName(name)
                .build()
        }
    }
    fun getDataUseProto():Flow<User>{
        return userDataStore.data.map { it }
    }

    object UserSerializer:Serializer<User>{
        override val defaultValue: User
            get() = User.getDefaultInstance()

        override suspend fun readFrom(input: InputStream): User {
            try {
                return User.parseFrom(input)
            }catch (e: InvalidProtocolBufferException){
                throw CorruptionException("Cannot read proto.", e)
            }
        }

        override suspend fun writeTo(t: User, output: OutputStream) {
            t.writeTo(output)
        }
    }
    val userDataStore:DataStore<User> by dataStore(fileName = "user.pb",serializer = UserSerializer)
}