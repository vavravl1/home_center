import React from "react";
// import {browserHistory, IndexRoute, Link, Route, Router} from "react-router"; //hashHistory
import ReactDOM from "react-dom";
import HomeCenterLayout from "./home-center-layout.jsx";


// if(document.getElementById('router_path').value) {
//     browserHistory.push(document.getElementById('router_path').value)
// }


ReactDOM.render(
    <HomeCenterLayout/>,
    document.getElementById('reactView')
);
