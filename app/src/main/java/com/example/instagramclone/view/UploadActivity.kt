package com.example.instagramclone.view

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.instagramclone.databinding.ActivityUploadBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.util.UUID

class UploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage : FirebaseStorage
    var selectedPicture : Uri? =null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        registerlauncher()
        auth  = Firebase.auth
        firestore = Firebase.firestore
        storage = Firebase.storage
    }

    fun upload (view:View) {
        binding.loading.visibility=View.VISIBLE
        val uuid = UUID.randomUUID()
        val imageName = "$uuid.jpg"

        val reference = storage.reference
        val imagereference =reference.child("images").child(imageName)
        if (selectedPicture!=null){
            imagereference.putFile(selectedPicture!!).addOnSuccessListener{
            val uploadPictureReference = storage.reference.child("images").child(imageName)
                uploadPictureReference.downloadUrl.addOnSuccessListener {
                    val downloadUrl = it.toString()
                    if(auth.currentUser != null) {

                        val postMap = hashMapOf<String, Any>()
                        postMap.put("downloadUrl", downloadUrl)
                        postMap.put("UserEmail", auth.currentUser!!.email!!)
                        postMap.put("comment", binding.commentText.text.toString())
                        postMap.put("date",com.google.firebase.Timestamp.now())

                        firestore.collection("Posts").add(postMap).addOnSuccessListener {
                            binding.loading.visibility=View.GONE
                            finish()
                        }.addOnFailureListener {
                            binding.loading.visibility=View.GONE
                            Toast.makeText(this,it.localizedMessage,Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }.addOnFailureListener{
                binding.loading.visibility=View.GONE
                Toast.makeText(this,it.localizedMessage,Toast.LENGTH_LONG).show()
            }
        }
    }

    fun SelectImage (view:View) {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give permission"){
                   // request permission
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }.show()
            }
            else{
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }else{
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            //start activity for result
            activityResultLauncher.launch(intentToGallery)
        }

    }
    private fun registerlauncher(){
        activityResultLauncher= registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
            if(result.resultCode == RESULT_OK){
                val intentFromResult = result.data
                if(intentFromResult!=null){
                    selectedPicture=  intentFromResult.data
                    selectedPicture?.let{
                        binding.imageView.setImageURI(it)
                    }
                }
            }

        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result->
            if(result){
                //permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
            else{
                //permission denied
                Toast.makeText(this@UploadActivity,"Permission needed!",Toast.LENGTH_LONG).show()
            }

        }
    }
}