import json
import os
import psycopg2
from psycopg2.extras import RealDictCursor

def get_db_connection():
    return psycopg2.connect(
        host=os.environ.get('DB_HOST'),
        database=os.environ.get('DB_NAME'),
        user=os.environ.get('DB_USER'),
        password=os.environ.get('DB_PASSWORD'),
        port=os.environ.get('DB_PORT', '5432'),
        connect_timeout=5
    )

def handler(event, context):
    try:
        body = json.loads(event.get('body', '{}'))
        cpf = body.get('cpf')

        if not cpf:
            return {
                'statusCode': 400,
                'headers': {'Access-Control-Allow-Origin': '*'},
                'body': json.dumps({'message': 'CPF é obrigatório.'})
            }

        cpf_limpo = ''.join(filter(str.isdigit, str(cpf)))

        conn = get_db_connection()
        cursor = conn.cursor(cursor_factory=RealDictCursor)

        query = "SELECT id, nome, cpf FROM clientes WHERE cpf = %s LIMIT 1;"
        cursor.execute(query, (cpf_limpo,))
        usuario = cursor.fetchone()

        cursor.close()
        conn.close()

        if not usuario:
            return {
                'statusCode': 404,
                'headers': {'Access-Control-Allow-Origin': '*'},
                'body': json.dumps({'message': 'CPF não cadastrado.'})
            }

        return {
            'statusCode': 200,
            'headers': {'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({
                'message': 'Autenticado com sucesso.',
                'cliente': {
                    'id': usuario['id'],
                    'nome': usuario['nome'],
                    'cpf': usuario['cpf']
                }
            })
        }

    except Exception as e:
        print(f"Erro na execução da Lambda: {str(e)}")
        return {
            'statusCode': 500,
            'headers': {'Access-Control-Allow-Origin': '*'},
            'body': json.dumps({'message': 'Erro interno do servidor.'})
        }