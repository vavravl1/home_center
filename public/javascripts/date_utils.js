

var DateUtils = {
  epochToString: function (epochInMillis) {
      var date = new Date(epochInMillis);
      return (date.getMonth() + 1) + "/" +
      date.getDate() + "/" +
      date.getFullYear() + " " +
      date.getHours() + ":" +
      date.getMinutes() + ":" +
      date.getSeconds()
  }


};


