import React from "react";
import {browserHistory, IndexRoute, Link, Route, Router} from "react-router"; //hashHistory
import ReactDOM from "react-dom";
import HomeCenterLayout from "./home-center-layout.jsx";
import HomeCenterData from "./home-center-data.jsx";
import SignIn from "./home-center-sign-in.jsx";
import HomeCenterSettings from "./settings/home-center-settings.jsx";
import axios from "axios";


if(document.getElementById('router_path').value) {
    browserHistory.push(document.getElementById('router_path').value)
}

let csrfTokenName = document.getElementById('csrf_token_name').value;
let csrfTokenValue = document.getElementById('csrf_token_value').value;

axios.defaults.params = {};
axios.defaults.params[csrfTokenName] = csrfTokenValue;

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
