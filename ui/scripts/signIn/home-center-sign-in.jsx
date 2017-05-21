import React from "react";
import FormGroup from "react-bootstrap/lib/FormGroup";
import Col from "react-bootstrap/lib/Col";
import Button from "react-bootstrap/lib/Button";
import FormControl from "react-bootstrap/lib/FormControl";
import Checkbox from "react-bootstrap/lib/Checkbox";
import ControlLabel from "react-bootstrap/lib/ControlLabel";
import {withRouter} from "react-router-dom";

class SignIn extends React.Component {

    componentDidMount = () => {
        if (document.getElementById('router_path').value) {
            this.props.history.push(document.getElementById('router_path').value);
        }
    };

    render = () => {
        let csrfTokenName = document.getElementById('csrf_token_name').value;
        let csrfTokenValue = document.getElementById('csrf_token_value').value;

        let validationState = null;

        const params = new URLSearchParams(this.props.location.search);
        const errorParam = params.get('error'); // bar

        if (errorParam === 'invalid.credentials') {
            validationState = "error"
        }

        return <Col xs={6} md={5}>
            <form action={"/signIn?" + csrfTokenName + "=" + csrfTokenValue} method="POST">
                <FormGroup controlId="formHorizontalEmail" validationState={validationState}>
                    <ControlLabel>Email</ControlLabel>
                    <FormControl type="email" placeholder="Email" name="email"/>
                </FormGroup>

                <FormGroup controlId="formHorizontalPassword" validationState={validationState}>
                    <ControlLabel>Password</ControlLabel>
                    <FormControl type="password" placeholder="Password" name="password"/>
                </FormGroup>

                <FormGroup>
                    <Checkbox name="rememberMe" value="true">Remember me</Checkbox>
                </FormGroup>

                <FormGroup>
                    <Col smOffset={2} sm={10}>
                        <Button type="submit">
                            Sign in
                        </Button>
                    </Col>
                </FormGroup>
            </form>
        </Col>
    }
}

export default withRouter(SignIn)