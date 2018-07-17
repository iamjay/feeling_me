'use strict';
let AWS = require('aws-sdk');
let comprehend = new AWS.Comprehend();

module.exports.get = (event, context, callback) => {
  console.log(`input : ${event}`);
  let q = event.q.replace(/%20/g, " ");;
  getSentiment(q).then(res=>{
    console.log(res);
    getKeywords(q).then(res=>{
      console.log(res);
      callback(null, res);
    })
  })
};

function getSentiment(input){
  return new Promise((resolve, reject) => {
    comprehend.detectSentiment({"LanguageCode":"en", "Text":input}, (err,res) => {
      let topSent = sortTopSentiment(res);
      topSent = findTwo(topSent);
      return resolve(topSent);
    })
  })
}

function sortTopSentiment(sen){
  let score = sen.SentimentScore;
  let scoreArr = new Array();
  for (let key in score) {
    if (score.hasOwnProperty(key)) {
      scoreArr.push([score[key],key]);
    }
  }
  return scoreArr.sort().reverse();
}

function findTwo(anyArr){
  let arr = new Array();
  arr.push(anyArr[0], anyArr[1]);
  return arr;
}

function getKeywords(input){
  return new Promise((resolve, reject) => {
    comprehend.detectKeyPhrases({"LanguageCode":"en", "Text":input}, (err,res) => {
      let topkeys = sortTopKeys(res);
      topkeys = findTwo(topkeys);
      return resolve(topkeys);
    })
  })
}

function sortTopKeys(keywords){
  let keywordsArr = new Array();
  keywords.KeyPhrases.forEach((el) => {
    keywordsArr.push([el.Score, el.Text]);
  })
  // let keywordsArr = new Array();
  // for (let key in keywords) {
  //   if (keywords.hasOwnProperty(key)) {
  //     keywordsArr.push([keywords[key],key]);
  //   }
  // }
  return keywordsArr.sort().reverse();
}


  // const response = {
  //   statusCode: 200,
  //   body: JSON.stringify({
  //     message: 'Go!! Serverless v1.0! Your function executed successfully!',
  //     input: event,
  //   }),
  // };

  // callback(null, response);

  // Use this code if you don't use the http event with the LAMBDA-PROXY integration
  // callback(null, { message: 'Go Serverless v1.0! Your function executed successfully!', event });
