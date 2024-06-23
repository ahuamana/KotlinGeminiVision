# Geminiai Multimodel

This is an Android project that uses AI to generate content from images.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

- Android Studio Koala | 2024.1.1 Canary 3 or later
- JDK 8 or later
- Android SDK

### Installing

1. Clone the repository:

```bash
git clone https://github.com/antonyhuaman/geminiai_multimodel.git
```

### API Key

This project uses an API key for some of its features. The API key is stored in a `local.properties` file in the root of the project. This file is not checked into version control, so you will need to create it yourself.

The `API_KEY` is used to authenticate requests made to the Generative AI service. This service is used to generate content from images in the app. 

Create a `local.properties` file in the root of the project and add your API key like this:

```ini
API_KEY=your_api_key_here
```
