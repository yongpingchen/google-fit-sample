# google-fit-sample
This article focus on reading height, weight and steps of users from Google Fit platform. 

These data should be recorded by other apps or wearable devices. 

Overview of [Google Fit Platform](https://developers.google.com/fit/overview)
### Prepare datas
You can download Google Fit app from Google Play Store. 
- Login with your google account(testing account) for health datas.
- Input your height and weight. 
- Walk out some while for step count. :smile: 

### Before Writing code
#### Prepare an App
1. Get a google account for development (can be different with testing account)
1. Create OAuth Client ID
    Refer to [official document](https://developers.google.com/fit/rest/v1/get-started#request_an_oauth_20_client_id)

#### Check DataType and Scope
Google Fit organize datas by DataType under Scopes. The Scope is used for authorize your app to read/write for some kind of data.
 
##### 1. check the data types.
My sample app would like to read height, weight and steps. These datas are public type data refer to [Public Data Types](https://developers.google.com/fit/rest/v1/data-types#public_data_types). From the table I know types of these datas: com.google.height, com.google.weight, and com.google.step_count.delta. 

##### 2. check the scope needed
Each scope provides access to a set of fitness data types. You should add scope when your app require to access these data from user. 
    Check the scope needed from this table of [google documentation](https://developers.google.com/fit/android/authorization#scopes). 
   My sample app only need to read the datas. So the codes needed are: 

| Scope | DataType |
|----|----|
| FITNESS_ACTIVITY_READ | com.google.step_count.delta|
| FITNESS_BODY_READ |com.google.height  |
| FITNESS_BODY_READ |com.google.weight  |
