import React from "react";
import Navbar from "react-bootstrap/lib/Navbar";
import LinkContainer from "react-router-bootstrap/lib/LinkContainer";
import Nav from "react-bootstrap/lib/Nav";
import NavItem from "react-bootstrap/lib/NavItem";
import {BrowserRouter, Link, Match, Route, Switch} from "react-router-dom";
import HomeCenterData from "../sensor/home-center-data.jsx";
import SignIn from "../signIn/home-center-sign-in.jsx";
import HomeCenterSettings from "../settings/home-center-settings.jsx";
import Actuator from "../actuator/actuator.jsx";
import axios from "axios";

class HomeCenterLayout extends React.Component {
    constructor(props) {
        super(props);
        this.state = {};
    };

    componentDidMount = () => {
        let csrfTokenName = document.getElementById('csrf_token_name').value;
        let csrfTokenValue = document.getElementById('csrf_token_value').value;

        axios.defaults.params = {};
        axios.defaults.params[csrfTokenName] = csrfTokenValue;
    };


    render = () => {
        let userNavItem = null;
        let user = document.getElementById('username').value;
        if (user) {
            userNavItem =
                <Nav pullRight>
                    <NavItem>{user}</NavItem>
                    <NavItem eventKey={1} href="signOut">Logout</NavItem>
                </Nav>
        } else {
            userNavItem =
                <Nav pullRight>
                    <LinkContainer to="/signIn"><NavItem>Login</NavItem></LinkContainer>
                </Nav>
        }
        return <BrowserRouter>
            <div>
                <Navbar>
                    <Navbar.Header>
                        <Navbar.Brand>
                            <Link to='data' className='navbar-brand'>Home center</Link>
                        </Navbar.Brand>
                    </Navbar.Header>
                    <Nav>
                        <LinkContainer to="/data"><NavItem>Sensors</NavItem></LinkContainer>
                    </Nav>
                    <Nav>
                        <LinkContainer to="/actions"><NavItem>Actuators</NavItem></LinkContainer>
                    </Nav>
                    <Nav>
                        <LinkContainer to="/settings"><NavItem>Settings</NavItem></LinkContainer>
                    </Nav>
                    {userNavItem}
                </Navbar>

                <Switch>
                    <Route exact path="/" component={HomeCenterData}/>
                    <Route path="/data" component={HomeCenterData}/>
                    <Route path="/actions" component={Actuator}/>
                    <Route path="/signIn" component={SignIn}/>
                    <Route path="/settings" component={HomeCenterSettings}/>
                </Switch>

            </div>
        </BrowserRouter>
    }
}

export default HomeCenterLayout;
