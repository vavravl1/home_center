import React from "react";
import {Router, Route, Link, IndexRoute, browserHistory} from 'react-router' //hashHistory
import ReactDOM from "react-dom";
import HomeCenterLayout from "./home-center-layout.jsx";
import HomeCenterData from "./home-center-data.jsx";
import SignIn from "./home-center-sign-in.jsx";
import HomeCenterSettings from "./home-center-settings.jsx";

if(document.getElementById('router_path').value) {
    browserHistory.push(document.getElementById('router_path').value)
}

ReactDOM.render(
    <Router history = {browserHistory}>
        <Route path="/" component={HomeCenterLayout}>
            <IndexRoute component={HomeCenterData}/>
            <Route path="data" component={HomeCenterData}/>
            <Route path="signIn" component={SignIn}/>
            <Route path="settings" component={HomeCenterSettings}/>
        </Route>
    </Router>,
    document.getElementById('reactView'));
