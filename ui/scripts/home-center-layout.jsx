import React from "react";
import Navbar from "react-bootstrap/lib/Navbar";
import LinkContainer from "react-router-bootstrap/lib/LinkContainer";
import Nav from "react-bootstrap/lib/Nav";
import NavItem from "react-bootstrap/lib/NavItem";
import { Link } from 'react-router';

class HomeCenterLayout extends React.Component {
    constructor(props) {
        super(props);
        this.state = {};
    };

    render = () => {
        let userNavItem = null;
        let user = document.getElementById('username').value;
        if (user) {
            userNavItem =
                <Nav pullRight>
                    <LinkContainer to="/data"><NavItem>{user}</NavItem></LinkContainer>
                    <NavItem eventKey={1} href="signOut">Logout</NavItem>
                </Nav>
        } else {
            userNavItem =
                <Nav pullRight>
                    <LinkContainer to="/signIn"><NavItem>Login</NavItem></LinkContainer>
                </Nav>
        }
        return <div>
            <Navbar>
                <Navbar.Header>
                    <Navbar.Brand>
                        <Link to='data' className='navbar-brand'>Home center</Link>
                    </Navbar.Brand>
                </Navbar.Header>
                {userNavItem}
            </Navbar>
            {this.props.children}
        </div>
    }
}

export default HomeCenterLayout;
