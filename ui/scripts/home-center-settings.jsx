import React from "react";
import Table from "react-bootstrap/lib/Table";
import Col from "react-bootstrap/lib/Col";

class HomeCenterSettings extends React.Component {

    render = () => {
        let csrfTokenName = document.getElementById('csrf_token_name').value;
        let csrfTokenValue = document.getElementById('csrf_token_value').value;

        return <Col xs={6} md={5}>
            <Table striped bordered condensed hover>
                <thead>
                <tr>
                    <th>#</th>
                    <th>First Name</th>
                    <th>Last Name</th>
                    <th>Username</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>1</td>
                    <td>Mark</td>
                    <td>Otto</td>
                    <td>@mdo</td>
                </tr>
                <tr>
                    <td>2</td>
                    <td>Jacob</td>
                    <td>Thornton</td>
                    <td>@fat</td>
                </tr>
                <tr>
                    <td>3</td>
                    <td colSpan="2">Larry the Bird</td>
                    <td>@twitter</td>
                </tr>
                </tbody>
            </Table>
        </Col>
    }
}

export default HomeCenterSettings