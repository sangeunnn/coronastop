# Corona Stop

#### ENV:

			Android 8+ 

			Android Device or Android Simulator

#### Need:

			Modify google Map Key in Manifest

#### Function:

<img src="/img/login.png" align="left" alt="login" width="300" height="500" />

[**loginActivity**]

1. ID
	Input Your ID

2. Password
	Input Your Password

3. Login Button 

4. Register Button
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<img src="/img/register.png" align="right" alt="register" width="300" height="500"/>

[**RegisterActivity**]

1.Enter the ID you want.
  
2.Confirm ID duplication
  
3.Enter the password you want
  
4.Confirm Password
  
5.Register Button
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<br>
<img src="/img/main.png" align ="left" alt="main" width="300" height="500"/>[**MainActivty**]

1.FIND ME Button
  
​	It shows your location and the corona patient's movement.
  
2.I GOT CORONA
  
​	If the user is caught in the corona, go to the activity to check the medical certificate to put it on the patient's movement Database.
  
Check the user's location every 10 minutes, and if the user exists within 500 meters around the corona patient movement,
Send a push alarm.
<br>  
<br>  
<br>  
<br>
<br>
<br>
<br>
<br>
<br>

<img src="/img/upload.png" align="right" alt="upload" width="300" height="500"/>

[**UploadActivty**]

1.Select medical Certificate image

Select a medical certificate from the gallery.
  
  
  
2.Upload Image to FireStorage(**Unimplemented**)
  
Send user's certificate to Firebase Storage. The manager checks the authenticity of the medical certificate and uploads the movement to the patient database[**Unimplemented**]. In the future, this function can be replaced by the vision API.**But now It goes directly to the database without checking for authenticity.**
  






















