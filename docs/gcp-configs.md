# GCP 


*   First you have to have a gcp account (free trial is ok all services that we need)
*   Create a [project](https://cloud.google.com/resource-manager/docs/creating-managing-projects) 
*   Create a service account, [more about it](https://cloud.google.com/compute/docs/access/service-accounts)
    *   Create a service account key : [json-key](https://console.cloud.google.com/apis/credentials/serviceaccountkey)
    *   Add role cloud spanner [here](https://console.cloud.google.com/iam-admin/iam) and to the correct member : name@project-id.iam.gserviceaccount.com
    *   [more about service account](https://cloud.google.com/compute/docs/access/service-accounts)

*   Create a [project](https://cloud.google.com/resource-manager/docs/creating-managing-projects) 

*   Download gcloud command line [here](https://cloud.google.com/sdk/docs/#install_the_latest_cloud_tools_version_cloudsdk_current_version)
*   Connect your service account to your gcloud : 
    ``gcloud auth login youremail@gmail.com``
    And your project : ``` gcloud config set project project-id```
